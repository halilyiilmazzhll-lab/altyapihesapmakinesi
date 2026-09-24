package com.example.egimhesabi.data

import android.content.Context
import android.net.Uri
import java.io.File

class WorkOrderPhotoStore(context: Context) {
    private val appContext = context.applicationContext
    private val permanentRoot = File(appContext.filesDir, PERMANENT_DIRECTORY).apply { mkdirs() }
    private val draftRoot = File(appContext.cacheDir, DRAFT_DIRECTORY).apply { mkdirs() }
    private val permanentCanonicale = permanentRoot.canonicalFile
    private val draftCanonicale = draftRoot.canonicalFile

    fun stage(uri: Uri, manholeId: Long): String {
        require(manholeId > 0) { "Fotoğraf geçerli bir bacaya bağlanmalıdır." }
        val target = File(draftRoot, "baca_${manholeId}_${System.nanoTime()}.img")
        try {
            appContext.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Fotoğraf açılamadı." }
                target.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 20 * 1024 * 1024) { "Bir fotoğraf en fazla 20 MB olabilir." }
                        output.write(buffer, 0, count)
                    }
                }
            }
        } catch (error: Exception) {
            target.delete()
            throw error
        }
        return target.canonicalPath
    }

    fun commitDrafts(paths: List<String>, manholeId: Long): List<String> {
        require(manholeId > 0)
        val manholeDirectory = File(permanentRoot, "baca_$manholeId").apply { mkdirs() }
        val created = mutableListOf<String>()
        try {
            return paths.distinct().map { path ->
                val source = File(path).canonicalFile
                require(source.isFile) { "Fotoğraf bulunamadı. Lütfen yeniden seçin." }
                if (source.isInside(permanentCanonicale)) source.path else {
                    require(source.isInside(draftCanonicale)) { "Geçersiz fotoğraf yolu." }
                    val target = File(manholeDirectory, "emir_${java.util.UUID.randomUUID()}.img").canonicalFile
                    created += target.path
                    source.copyTo(target, overwrite = false)
                    target.path
                }
            }
        } catch (error: Exception) {
            deletePermanent(created)
            throw error
        }
    }

    fun discardDrafts(paths: Iterable<String>) {
        paths.forEach { path ->
            runCatching {
                File(path).canonicalFile
                    .takeIf { it.isInside(draftCanonicale) && it.isFile }
                    ?.delete()
            }
        }
    }

    fun deletePermanent(paths: Iterable<String>) {
        paths.forEach { path ->
            runCatching {
                File(path).canonicalFile
                    .takeIf { it.isInside(permanentCanonicale) && it.isFile }
                    ?.delete()
            }
        }
        permanentRoot.listFiles()
            ?.filter(File::isDirectory)
            ?.filter { it.list()?.isEmpty() == true }
            ?.forEach(File::delete)
    }

    fun reconcile(referencedPaths: Set<String>) {
        val canonicalReferences = referencedPaths.mapNotNullTo(mutableSetOf()) { path ->
            runCatching { File(path).canonicalPath }.getOrNull()
        }
        permanentRoot.walkTopDown()
            .filter(File::isFile)
            .filter { it.canonicalPath !in canonicalReferences && it.lastModified() < System.currentTimeMillis() - 86_400_000 }
            .forEach(File::delete)
        val draftFiles = draftRoot.listFiles()?.asIterable() ?: emptyList()
        discardDrafts(draftFiles.filter { it.lastModified() < System.currentTimeMillis() - 86_400_000 }.map { it.path })
    }

    private fun File.isInside(parent: File): Boolean =
        path == parent.path || path.startsWith(parent.path + File.separator)

    private companion object {
        const val PERMANENT_DIRECTORY = "work_order_photos"
        const val DRAFT_DIRECTORY = "work_order_photo_drafts"
    }
}
