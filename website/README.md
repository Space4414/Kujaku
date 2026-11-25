# Kujaku Website

A premium, responsive multi-page static website for Kujaku. Built with semantic HTML, CSS and vanilla JavaScript so it can be hosted directly on Vercel without a build step.

## Deploy on Vercel

1. Import the `Space4414/Kujaku` GitHub repository into Vercel.
2. Set **Root Directory** to `website`.
3. Leave Framework Preset as **Other** and Build Command empty. Output Directory can remain empty (or `.`).
4. Deploy. Vercel will serve the static pages in this folder.

The site fetches release metadata and APK assets from the public GitHub Releases API at runtime. The changelog and download listings therefore stay current without manual edits.

## Pages

- `/index.html` Home
- `/features.html` Features
- `/download.html` Official APK downloads
- `/changelog.html` GitHub-powered release notes
- `/faq.html` Searchable FAQ based on the app's FAQ, with additional answers
- `/privacy.html` Privacy policy

Before using a custom domain, replace `https://kujaku.vercel.app` in canonical links, Open Graph metadata, robots.txt and sitemap.xml with the final production URL.
