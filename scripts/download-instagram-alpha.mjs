#!/usr/bin/env node

/**
 * Instafel Autonomous Alpha Crawler & Downloader
 * 
 * Automatically discovers, resolves, and streams the latest Instagram Alpha
 * (arm64-v8a) package from APKMirror directly into the CI workspace.
 */

import { createWriteStream, mkdirSync, existsSync } from 'fs';
import { pipeline } from 'stream/promises';
import { appendFileSync } from 'fs';

const BASE_URL = 'https://www.apkmirror.com';
const ORG_SLUG = 'instagram';
const REPO_SLUG = 'instagram-instagram';

const fetchHeaders = async (url, args = {}) => {
  args.headers = args.headers || {};
  args.headers['user-agent'] = 'APKUpdater-v0';
  args.headers['authorization'] = 'Basic YXBpLWFwa3VwZGF0ZXI6cm01cmNmcnVVakt5MDRzTXB5TVBKWFc4';
  return fetch(url, args);
};

const writeGithubOutput = (key, value) => {
  const outputFile = process.env.GITHUB_OUTPUT;
  if (outputFile) {
    appendFileSync(outputFile, `${key}=${value}\n`);
  }
};

async function getLatestAlphaRelease() {
  console.log(`[Crawler] Fetching releases list from ${BASE_URL}/apk/${ORG_SLUG}/${REPO_SLUG}/...`);
  const res = await fetchHeaders(`${BASE_URL}/apk/${ORG_SLUG}/${REPO_SLUG}/`);
  if (!res.ok) {
    throw new Error(`Failed to fetch releases list: HTTP ${res.status}`);
  }
  const html = await res.text();

  // Match all release links: /apk/instagram/instagram-instagram/instagram-<version>-release/
  const regex = /href="(\/apk\/instagram\/instagram-instagram\/instagram-([0-9]+-[0-9]+-[0-9]+-[0-9]+-[0-9]+)-release\/)"/g;
  const matches = [...html.matchAll(regex)];

  if (matches.length === 0) {
    throw new Error('No Instagram release links found on the main APKMirror page.');
  }

  // Deduplicate matches
  const releaseMap = new Map();
  for (const m of matches) {
    const link = m[1];
    const rawVersion = m[2]; // e.g. 451-0-0-0-70
    const semver = rawVersion.replaceAll('-', '.');
    if (!releaseMap.has(semver)) {
      releaseMap.set(semver, link);
    }
  }

  const releases = Array.from(releaseMap.entries()).map(([version, link]) => ({
    version,
    link: `${BASE_URL}${link}`
  }));

  console.log(`[Crawler] Found ${releases.length} recent releases:`);
  for (const r of releases.slice(0, 5)) {
    console.log(`  - v${r.version} (${r.link})`);
  }

  // Alpha versions typically have the highest major version code (e.g. 451 vs 450 vs 449)
  // Sort descending by numeric version comparison
  releases.sort((a, b) => {
    const pa = a.version.split('.').map(Number);
    const pb = b.version.split('.').map(Number);
    for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
      const na = pa[i] || 0;
      const nb = pb[i] || 0;
      if (na !== nb) return nb - na;
    }
    return 0;
  });

  const latestAlpha = releases[0];
  console.log(`[Crawler] Selected latest Alpha build: v${latestAlpha.version}`);
  return latestAlpha;
}

async function getArm64Variant(release) {
  console.log(`[Crawler] Fetching variants table for v${release.version}...`);
  const res = await fetchHeaders(release.link);
  if (!res.ok) {
    throw new Error(`Failed to fetch variants page: HTTP ${res.status}`);
  }
  const html = await res.text();

  // Find variant download pages
  // e.g. /apk/instagram/instagram-instagram/instagram-451-0-0-0-70-release/instagram-451-0-0-0-70-2-android-apk-download/
  const regex = /href="(\/apk\/instagram\/instagram-instagram\/[^\"]+android-apk-download\/)"/g;
  const matches = [...html.matchAll(regex)];
  const variantLinks = Array.from(new Set(matches.map(m => `${BASE_URL}${m[1]}`)));

  if (variantLinks.length === 0) {
    throw new Error(`No variant download links found for release v${release.version}`);
  }

  console.log(`[Crawler] Found ${variantLinks.length} variants available.`);

  // Prefer variant 2 (often 480-640dpi arm64-v8a) or variant 1
  let targetVariant = variantLinks[0];
  for (const link of variantLinks) {
    if (link.includes('-2-android-apk-download')) {
      targetVariant = link;
      break;
    }
  }

  console.log(`[Crawler] Selected target variant: ${targetVariant}`);
  return targetVariant;
}

async function resolveDirectDownloadUrl(variantUrl) {
  console.log(`[Crawler] Step 1: Navigating to variant page...`);
  const r1 = await fetchHeaders(variantUrl);
  if (!r1.ok) {
    throw new Error(`Failed to open variant page: HTTP ${r1.status}`);
  }
  const text1 = await r1.text();

  const downloadPageMatch = text1.match(/href="(\/apk\/[^\"]+android-apk-download\/download\/[^\"]*)"/);
  if (!downloadPageMatch) {
    throw new Error('Could not find intermediate download page button on variant page.');
  }

  const downloadPageUrl = `${BASE_URL}${downloadPageMatch[1]}`;
  console.log(`[Crawler] Step 2: Navigating to download staging page (${downloadPageUrl})...`);

  const r2 = await fetchHeaders(downloadPageUrl, {
    headers: { referer: variantUrl }
  });
  if (!r2.ok) {
    throw new Error(`Failed to open download staging page: HTTP ${r2.status}`);
  }
  const text2 = await r2.text();

  const finalMatch = text2.match(/href="([^"]*wp-content\/themes\/APKMirror\/download\.php[^"]*)"/);
  if (!finalMatch) {
    throw new Error('Could not find download.php link on download staging page.');
  }

  const finalDlUrl = `${BASE_URL}${finalMatch[1]}`;
  console.log(`[Crawler] Step 3: Resolved direct download URL: ${finalDlUrl}`);
  return { finalDlUrl, referer: downloadPageUrl };
}

async function downloadFile(url, referer, destPath) {
  console.log(`[Downloader] Starting download to ${destPath}...`);
  const res = await fetchHeaders(url, {
    headers: { referer }
  });

  if (!res.ok) {
    throw new Error(`Download request failed: HTTP ${res.status}`);
  }

  const contentType = res.headers.get('content-type') || '';
  const contentLength = Number(res.headers.get('content-length')) || 0;
  console.log(`[Downloader] Content-Type: ${contentType}, Size: ${(contentLength / (1024 * 1024)).toFixed(2)} MB`);

  const fileStream = createWriteStream(destPath);
  let downloadedBytes = 0;
  let lastLoggedMB = 0;

  const reader = res.body.getReader();
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    fileStream.write(value);
    downloadedBytes += value.length;
    const currentMB = Math.floor(downloadedBytes / (10 * 1024 * 1024)) * 10;
    if (currentMB > lastLoggedMB) {
      lastLoggedMB = currentMB;
      const pct = contentLength ? ` (${((downloadedBytes / contentLength) * 100).toFixed(1)}%)` : '';
      console.log(`[Downloader] Downloaded ${(downloadedBytes / (1024 * 1024)).toFixed(2)} MB${pct}...`);
    }
  }

  fileStream.end();
  console.log(`[Downloader] Successfully completed download: ${(downloadedBytes / (1024 * 1024)).toFixed(2)} MB saved to ${destPath}`);
}

async function main() {
  try {
    const targetDir = process.env.DOWNLOAD_DIR || 'downloads';
    if (!existsSync(targetDir)) {
      mkdirSync(targetDir, { recursive: true });
    }

    // Step 1: Discover latest Alpha
    const release = await getLatestAlphaRelease();
    writeGithubOutput('version', release.version);

    // Step 2: Get target arm64-v8a variant
    const variantUrl = await getArm64Variant(release);

    // Step 3: Resolve final download URL with referer
    const { finalDlUrl, referer } = await resolveDirectDownloadUrl(variantUrl);

    // Step 4: Stream download to disk
    const destPath = `${targetDir}/instagram_bundle.apkm`;
    await downloadFile(finalDlUrl, referer, destPath);

    writeGithubOutput('file_path', destPath);
    console.log(`\n[Success] Instagram Alpha v${release.version} is ready at ${destPath}!`);
    process.exit(0);
  } catch (err) {
    console.error(`\n[Error] Alpha Crawler failed: ${err.message}`);
    process.exit(1);
  }
}

main();
