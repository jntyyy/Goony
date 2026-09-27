package com.example.network

import android.util.Log
import com.example.data.local.entity.CoomerPostData
import com.example.data.local.entity.HanimeEpisodeData
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.regex.Pattern

data class ScrapedGalleryResult(
    val title: String,
    val coverImage: String?,
    val images: List<String>
)

data class ScrapedHanimeResult(
    val title: String,
    val coverImage: String,
    val description: String,
    val censorship: String,
    val episodes: List<HanimeEpisodeData>,
    val secondaryCovers: List<String>
)

data class ScrapedCreatorResult(
    val name: String,
    val avatarUrl: String,
    val posts: List<CoomerPostData>,
    val service: String
)

data class ScrapedTorrent(
    val title: String,
    val magnetUrl: String,
    val size: String,
    val seeders: Int,
    val leechers: Int,
    val siteName: String
)

object MediaScrapers {
    private const val TAG = "MediaScrapers"

    // 1. Photoset Scraper (AdultPhotoSets, PornBox, FreeOnes, direct image list)
    suspend fun scrapeGallery(url: String): ScrapedGalleryResult {
        return try {
            val html = NetworkClient.getHtml(url)
            val doc = Jsoup.parse(html)
            val title = doc.title().replace(" - AdultPhotoSets", "").replace(" - FreeOnes", "").trim()
            val images = mutableListOf<String>()

            // Extract all image links and fullsize img tags
            val elements = doc.select("a[href~=(?i)\\.(png|jpe?g|webp)], img[src~=(?i)\\.(png|jpe?g|webp)], img[data-src~=(?i)\\.(png|jpe?g|webp)]")
            for (el in elements) {
                var imgUrl = el.attr("href")
                if (imgUrl.isEmpty() || !imgUrl.matches(Regex(".*\\.(jpg|jpeg|png|webp).*", RegexOption.IGNORE_CASE))) {
                    imgUrl = el.attr("data-src")
                }
                if (imgUrl.isEmpty()) {
                    imgUrl = el.attr("src")
                }
                if (imgUrl.isNotEmpty() && !imgUrl.contains("logo") && !imgUrl.contains("banner") && !imgUrl.contains("icon")) {
                    val fullUrl = if (imgUrl.startsWith("//")) "https:$imgUrl"
                    else if (imgUrl.startsWith("/")) {
                        val base = url.substringBefore("/", url.removePrefix("https://"))
                        "https://$base$imgUrl"
                    } else imgUrl

                    if (!images.contains(fullUrl)) {
                        images.add(fullUrl)
                    }
                }
            }

            val cover = images.firstOrNull()
            ScrapedGalleryResult(title = title.ifEmpty { "Photoset Gallery" }, coverImage = cover, images = images)
        } catch (e: Exception) {
            Log.e(TAG, "scrapeGallery error", e)
            ScrapedGalleryResult("Photoset", null, emptyList())
        }
    }

    // 2. Coomer / OnlyFans / Fansly Creator Scraper
    suspend fun scrapeCreatorProfile(profileUrl: String): ScrapedCreatorResult {
        var name = "Creator"
        var avatarUrl = ""
        val posts = mutableListOf<CoomerPostData>()
        var service = "OnlyFans"

        try {
            // e.g. https://coomer.su/onlyfans/user/username
            val match = Pattern.compile("coomer\\.su/([a-zA-Z0-9]+)/user/([a-zA-Z0-9_.-]+)").matcher(profileUrl)
            val detectedService = if (match.find()) match.group(1) ?: "onlyfans" else "onlyfans"
            val detectedUser = if (match.groupCount() >= 2) match.group(2) ?: "" else ""
            service = detectedService.replaceFirstChar { it.uppercase() }
            name = detectedUser.ifEmpty { "Creator" }

            val html = NetworkClient.getHtml(profileUrl)
            val doc = Jsoup.parse(html)

            val avatarEl = doc.select(".user-header__avatar img, .fancy-image__image").firstOrNull()
            if (avatarEl != null) {
                avatarUrl = avatarEl.attr("src")
                if (avatarUrl.startsWith("//")) avatarUrl = "https:$avatarUrl"
            }

            val postElements = doc.select("article.post-card")
            for ((idx, el) in postElements.withIndex()) {
                val postId = el.attr("data-id").ifEmpty { "post_$idx" }
                val caption = el.select(".post-card__header").text()
                val mediaLinks = el.select("a.post-card__image-link, a.fileThumb")
                val urls = mutableListOf<String>()
                val thumbs = mutableListOf<String>()
                val mediaTypes = mutableListOf<String>()

                for (link in mediaLinks) {
                    val href = link.attr("href")
                    val thumb = link.select("img").attr("src")
                    if (href.isNotEmpty()) {
                        val fullMedia = if (href.startsWith("//")) "https:$href" else href
                        urls.add(fullMedia)
                        thumbs.add(if (thumb.startsWith("//")) "https:$thumb" else thumb)
                        mediaTypes.add(if (href.endsWith(".mp4") || href.endsWith(".m4v")) "video" else "image")
                    }
                }

                if (urls.isNotEmpty()) {
                    posts.add(
                        CoomerPostData(
                            id = postId,
                            urls = urls,
                            thumbUrls = thumbs,
                            caption = caption.ifEmpty { null },
                            mediaTypes = mediaTypes,
                            sourceService = service
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "scrapeCreatorProfile error", e)
        }

        return ScrapedCreatorResult(name = name, avatarUrl = avatarUrl, posts = posts, service = service)
    }

    // 3. Hanime Series Scraper
    suspend fun scrapeHanimeSeries(url: String): ScrapedHanimeResult {
        var title = "Hanime Series"
        var cover = ""
        var desc = ""
        var censorship = "UNCENSORED"
        val episodes = mutableListOf<HanimeEpisodeData>()
        val secondaryCovers = mutableListOf<String>()

        try {
            val html = NetworkClient.getHtml(url)
            val doc = Jsoup.parse(html)
            title = doc.select("h1").text().ifEmpty { "Hanime Series" }
            desc = doc.select(".description, .synopsis, p").firstOrNull()?.text() ?: ""
            val coverEl = doc.select("img.cover, .poster img").firstOrNull()
            if (coverEl != null) {
                cover = coverEl.attr("src")
            }

            val epLinks = doc.select("a[href*=/watch/]")
            for ((idx, ep) in epLinks.withIndex()) {
                val epUrl = ep.attr("href")
                val epTitle = ep.text().ifEmpty { "Episode ${idx + 1}" }
                val epThumb = ep.select("img").attr("src")
                val fullUrl = if (epUrl.startsWith("http")) epUrl else "https://hstream.moe$epUrl"

                episodes.add(
                    HanimeEpisodeData(
                        id = "ep_${idx + 1}",
                        url = fullUrl,
                        coverImage = epThumb.ifEmpty { cover },
                        episodeNumber = idx + 1,
                        title = epTitle
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "scrapeHanimeSeries error", e)
        }

        return ScrapedHanimeResult(
            title = title,
            coverImage = cover,
            description = desc,
            censorship = censorship,
            episodes = episodes,
            secondaryCovers = secondaryCovers
        )
    }

    // 4. Sukebei / Torrent Scraper
    suspend fun scrapeSukebei(query: String): List<ScrapedTorrent> {
        val list = mutableListOf<ScrapedTorrent>()
        try {
            val searchUrl = "https://sukebei.nyaa.si/?f=0&c=0_0&q=${query.replace(" ", "+")}"
            val html = NetworkClient.getHtml(searchUrl)
            val doc = Jsoup.parse(html)
            val rows = doc.select("tr.default, tr.success, tr.danger")

            for (row in rows) {
                val titleEl = row.select("td:nth-child(2) a:not(.comments)").lastOrNull()
                val magnetEl = row.select("a[href^=magnet:]").firstOrNull()
                val sizeEl = row.select("td:nth-child(4)").firstOrNull()
                val seedersEl = row.select("td:nth-child(6)").firstOrNull()
                val leechersEl = row.select("td:nth-child(7)").firstOrNull()

                if (titleEl != null && magnetEl != null) {
                    list.add(
                        ScrapedTorrent(
                            title = titleEl.text().trim(),
                            magnetUrl = magnetEl.attr("href"),
                            size = sizeEl?.text()?.trim() ?: "Unknown",
                            seeders = seedersEl?.text()?.trim()?.toIntOrNull() ?: 0,
                            leechers = leechersEl?.text()?.trim()?.toIntOrNull() ?: 0,
                            siteName = "Sukebei Nyaa"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "scrapeSukebei error", e)
        }
        return list
    }

    // 5. SubtitleCat Scraper
    suspend fun scrapeSubtitleCat(code: String): String? {
        return try {
            val url = "https://www.subtitlecat.com/index.php?search=${code.trim()}"
            val html = NetworkClient.getHtml(url)
            val doc = Jsoup.parse(html)
            val subLink = doc.select("a[href*=/subtitles/]").firstOrNull()?.attr("href")
            if (subLink != null) {
                val fullUrl = if (subLink.startsWith("http")) subLink else "https://www.subtitlecat.com$subLink"
                val pageHtml = NetworkClient.getHtml(fullUrl)
                val dlDoc = Jsoup.parse(pageHtml)
                val downloadLink = dlDoc.select("a[href*=/download/]").firstOrNull()?.attr("href")
                if (downloadLink != null) {
                    if (downloadLink.startsWith("http")) downloadLink else "https://www.subtitlecat.com$downloadLink"
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "scrapeSubtitleCat error", e)
            null
        }
    }
}
