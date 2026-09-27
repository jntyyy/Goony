package com.example.data.local.converters

import androidx.room.TypeConverter
import com.example.data.local.entity.CoomerPostData
import com.example.data.local.entity.HanimeEpisodeData
import org.json.JSONArray
import org.json.JSONObject

class RoomConverters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            val array = JSONArray(value)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromHanimeEpisodes(episodes: List<HanimeEpisodeData>?): String {
        if (episodes == null) return "[]"
        val array = JSONArray()
        for (ep in episodes) {
            val obj = JSONObject()
            obj.put("id", ep.id)
            obj.put("stashDbId", ep.stashDbId ?: JSONObject.NULL)
            obj.put("url", ep.url)
            obj.put("coverImage", ep.coverImage)
            obj.put("episodeNumber", ep.episodeNumber)
            obj.put("title", ep.title ?: JSONObject.NULL)
            obj.put("description", ep.description ?: JSONObject.NULL)
            obj.put("boosterCover", ep.boosterCover ?: JSONObject.NULL)
            obj.put("assignedDate", ep.assignedDate ?: JSONObject.NULL)
            obj.put("censorship", ep.censorship ?: JSONObject.NULL)
            val gallArr = JSONArray()
            ep.gallery.forEach { gallArr.put(it) }
            obj.put("gallery", gallArr)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toHanimeEpisodes(value: String?): List<HanimeEpisodeData> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            val array = JSONArray(value)
            val list = mutableListOf<HanimeEpisodeData>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val gallArr = obj.optJSONArray("gallery")
                val gall = mutableListOf<String>()
                if (gallArr != null) {
                    for (g in 0 until gallArr.length()) {
                        gall.add(gallArr.getString(g))
                    }
                }
                list.add(
                    HanimeEpisodeData(
                        id = obj.getString("id"),
                        stashDbId = if (obj.isNull("stashDbId")) null else obj.optString("stashDbId"),
                        url = obj.optString("url", ""),
                        coverImage = obj.optString("coverImage", ""),
                        episodeNumber = obj.optInt("episodeNumber", 1),
                        title = if (obj.isNull("title")) null else obj.optString("title"),
                        description = if (obj.isNull("description")) null else obj.optString("description"),
                        boosterCover = if (obj.isNull("boosterCover")) null else obj.optString("boosterCover"),
                        assignedDate = if (obj.isNull("assignedDate")) null else obj.optLong("assignedDate"),
                        censorship = if (obj.isNull("censorship")) null else obj.optString("censorship"),
                        gallery = gall
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromCoomerPosts(posts: List<CoomerPostData>?): String {
        if (posts == null) return "[]"
        val array = JSONArray()
        for (post in posts) {
            val obj = JSONObject()
            obj.put("id", post.id)
            obj.put("stashDbId", post.stashDbId ?: JSONObject.NULL)
            obj.put("caption", post.caption ?: JSONObject.NULL)
            obj.put("type", post.type)
            obj.put("createdAt", post.createdAt)
            obj.put("date", post.date ?: JSONObject.NULL)
            obj.put("sourceService", post.sourceService ?: JSONObject.NULL)

            val uArr = JSONArray()
            post.urls.forEach { uArr.put(it) }
            obj.put("urls", uArr)

            val tArr = JSONArray()
            post.thumbUrls.forEach { tArr.put(it) }
            obj.put("thumbUrls", tArr)

            val mArr = JSONArray()
            post.mediaTypes.forEach { mArr.put(it) }
            obj.put("mediaTypes", mArr)

            val dArr = JSONArray()
            post.videoDurations.forEach { dArr.put(it) }
            obj.put("videoDurations", dArr)

            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toCoomerPosts(value: String?): List<CoomerPostData> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            val array = JSONArray(value)
            val list = mutableListOf<CoomerPostData>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val uArr = obj.optJSONArray("urls")
                val urls = mutableListOf<String>()
                if (uArr != null) {
                    for (j in 0 until uArr.length()) urls.add(uArr.getString(j))
                }
                val tArr = obj.optJSONArray("thumbUrls")
                val thumbs = mutableListOf<String>()
                if (tArr != null) {
                    for (j in 0 until tArr.length()) thumbs.add(tArr.getString(j))
                }
                val mArr = obj.optJSONArray("mediaTypes")
                val mediaTypes = mutableListOf<String>()
                if (mArr != null) {
                    for (j in 0 until mArr.length()) mediaTypes.add(mArr.getString(j))
                }
                val dArr = obj.optJSONArray("videoDurations")
                val durations = mutableListOf<Double>()
                if (dArr != null) {
                    for (j in 0 until dArr.length()) durations.add(dArr.getDouble(j))
                }

                list.add(
                    CoomerPostData(
                        id = obj.getString("id"),
                        stashDbId = if (obj.isNull("stashDbId")) null else obj.optString("stashDbId"),
                        urls = urls,
                        thumbUrls = thumbs,
                        caption = if (obj.isNull("caption")) null else obj.optString("caption"),
                        mediaTypes = mediaTypes,
                        videoDurations = durations,
                        type = obj.optString("type", "Single"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        date = if (obj.isNull("date")) null else obj.optLong("date"),
                        sourceService = if (obj.isNull("sourceService")) null else obj.optString("sourceService")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
