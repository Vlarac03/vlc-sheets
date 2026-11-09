package com.vlcsheets.app.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class JpegPdfWriterTest {

    @Test
    fun crossReferenceTablePointsAtEveryObject() {
        val out = ByteArrayOutputStream()
        JpegPdfWriter(out).apply {
            addPage(595, 842, 1600, 2263, ByteArray(500) { 0xFF.toByte() })
            addPage(595, 400, 1600, 1075, ByteArray(300) { 7 })
            finish()
        }
        val pdf = String(out.toByteArray(), Charsets.ISO_8859_1)
        assertTrue(pdf.startsWith("%PDF-1.4"))
        assertTrue(pdf.endsWith("%%EOF\n"))
        val xref = pdf.substringAfterLast("startxref\n").substringBefore('\n').toInt()
        assertTrue(pdf.startsWith("xref\n0 9\n", xref))
        // Objects 1..8: catalog, page list, then image, content and page for each of the 2 pages.
        val lines = pdf.substring(xref).lines().drop(3).take(8)
        lines.forEachIndexed { i, line ->
            assertEquals(20, line.length + 1)
            assertTrue("object ${i + 1}", pdf.startsWith("${i + 1} 0 obj\n", line.take(10).toInt()))
        }
        assertTrue(pdf.contains("/Kids [5 0 R 8 0 R] /Count 2"))
    }
}
