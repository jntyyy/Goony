package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.data.local.converters.RoomConverters

@Entity(tableName = "links")
@TypeConverters(RoomConverters::class)
data class LinkEntity(
    @PrimaryKey val id: String,
    val stashDbId: String? = null,
    val title: String,
    val urlHD: String? = null,
    val url4K: String? = null,
    val magnet: String? = null,
    val magnet4K: String? = null,
    val torrentUrlHD: String? = null,
    val torrentUrl4K: String? = null,
    val torrentSiteName: String? = null,
    val coverImage: String = "",
    val coverOffset: Float = 50f,
    val aspectRatio: String = "16:9",
    val galleryUrls: List<String> = emptyList(),
    val originalGalleryUrls: List<String> = emptyList(),
    val qualityPreference: String = "original",
    val galleryScraperUrl: String? = null,
    val actorIds: List<String> = emptyList(),
    val studioIds: List<String> = emptyList(),
    val assignedDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "actors")
@TypeConverters(RoomConverters::class)
data class ActorEntity(
    @PrimaryKey val id: String,
    val stashDbId: String? = null,
    val name: String,
    val imageUrl: String = "",
    val originalImageUrl: String? = null,
    val imagePositionX: Float = 50f,
    val imagePositionY: Float = 50f,
    val imageZoom: Float = 1.0f,
    val instagramUrl: String? = null,
    val twitterUrl: String? = null,
    val onlyFansUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "studios")
data class StudioEntity(
    @PrimaryKey val id: String,
    val stashDbId: String? = null,
    val name: String,
    val imageUrl: String? = null,
    val logoUrl: String? = null,
    val logoBgColor: String? = null,
    val originalImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class HanimeEpisodeData(
    val id: String,
    val stashDbId: String? = null,
    val url: String = "",
    val coverImage: String = "",
    val episodeNumber: Int = 1,
    val title: String? = null,
    val description: String? = null,
    val boosterCover: String? = null,
    val assignedDate: Long? = null,
    val censorship: String? = null,
    val gallery: List<String> = emptyList()
)

@Entity(tableName = "hanime")
@TypeConverters(RoomConverters::class)
data class HanimeEntity(
    @PrimaryKey val id: String,
    val stashDbId: String? = null,
    val title: String,
    val coverImage: String = "",
    val coverOffset: Float = 50f,
    val secondaryCovers: List<String> = emptyList(),
    val description: String? = null,
    val assignedDate: Long? = null,
    val censorship: String = "UNCENSORED",
    val episodes: List<HanimeEpisodeData> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class CoomerPostData(
    val id: String,
    val stashDbId: String? = null,
    val urls: List<String> = emptyList(),
    val thumbUrls: List<String> = emptyList(),
    val caption: String? = null,
    val mediaTypes: List<String> = emptyList(),
    val videoDurations: List<Double> = emptyList(),
    val type: String = "Single",
    val createdAt: Long = System.currentTimeMillis(),
    val date: Long? = null,
    val perImageDates: List<Long?> = emptyList(),
    val sourceService: String? = null
)

@Entity(tableName = "coomers")
@TypeConverters(RoomConverters::class)
data class CoomerEntity(
    @PrimaryKey val id: String,
    val stashDbId: String? = null,
    val name: String,
    val imageUrl: String = "",
    val originalImageUrl: String? = null,
    val posts: List<CoomerPostData> = emptyList(),
    val totalPostsCount: Int = 0,
    val sourceUrl: String? = null,
    val secondarySourceUrl: String? = null,
    val sourceUrls: List<String> = emptyList(),
    val goonboxUrl: String? = null,
    val isFullySynced: Boolean = false,
    val imagePositionX: Float = 50f,
    val imagePositionY: Float = 50f,
    val imageZoom: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val currentTheme: String = "Dark",
    val accentColorHex: String = "#8B5CF6", // Violet accent
    val torboxApiKey: String = "",
    val realDebridApiKey: String = "HNR2RHUY4K6JYXNFJCB4QXAJ57TKDQKTQOPYEXZ2VANQO7TN5YJQ",
    val stashDbApiKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOiIwMTlmYmRlYi00MDRlLTdjYmMtOTFhNy00YTA4MjhjMTQ5ZjQiLCJzdWIiOiJBUElLZXkiLCJpYXQiOjE3ODU1OTc3Mzl9.J9ojzjsBP8sBOLZNUACF94EWwren89ql8TDcW3gT7WY",
    val geminiApiKey: String = "",
    val blurCovers: Boolean = false,
    val betaTestPrivacy: Boolean = false,
    val defaultAspectRatio: String = "16:9",
    val showManagementCards: Boolean = true,
    val appIconStyle: Int = 0,
    val transitionStyle: Int = 0,
    val lastSyncTime: Long = 0L
)
