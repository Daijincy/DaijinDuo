package com.daijinduo.github.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImageItem(val id: Long, val uri: Uri)

/**
 * 读取相册最近图片列表与缩略图。
 * 缩略图优先用系统 loadThumbnail（API 29+），旧版本走 MediaStore 缩略图。
 */
object GalleryLoader {

    const val LIMIT = 300

    suspend fun loadRecent(context: Context): List<ImageItem> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )
        // 注意：不使用 SQL "LIMIT"（部分设备 ROM 的 MediaProvider 不支持会抛异常），改在游标侧截断
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val items = ArrayList<ImageItem>(128)
        try {
            resolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext() && items.size < LIMIT) {
                    val id = cursor.getLong(idColumn)
                    items.add(ImageItem(id, ContentUris.withAppendedId(collection, id)))
                }
            }
        } catch (_: Exception) {
            // 查询失败（部分 ROM 权限/Provider 差异）时返回空列表，避免闪退
        }
        items
    }

    @Suppress("DEPRECATION")
    fun loadThumbnail(resolver: android.content.ContentResolver, uri: Uri, size: Int): Bitmap? =
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                resolver.loadThumbnail(uri, Size(size, size), null)
            } else {
                val id = ContentUris.parseId(uri)
                MediaStore.Images.Thumbnails.getThumbnail(
                    resolver,
                    id,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null
                )
            }
        } catch (_: Exception) {
            null
        }
}
