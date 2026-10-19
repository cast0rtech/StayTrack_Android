package com.staytrack.core.export

import com.staytrack.core.model.Place
import com.staytrack.core.model.Visit
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Genera hojas de cálculo genuinas ECMA-376 OpenXML (.xlsx) sin dependencias externas pesadas.
 * Compatible directamente con Microsoft Excel, Google Sheets y LibreOffice.
 */
object XlsxExporter {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    fun exportToStream(visitas: List<Visit>, lugares: List<Place>, outputStream: OutputStream) {
        val placeMap = lugares.associateBy { it.id }
        val sheetXml = buildSheetXml(visitas, placeMap)

        ZipOutputStream(outputStream).use { zip ->
            addZipEntry(zip, "[Content_Types].xml", buildContentTypesXml())
            addZipEntry(zip, "_rels/.rels", buildRootRelsXml())
            addZipEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml())
            addZipEntry(zip, "xl/workbook.xml", buildWorkbookXml())
            addZipEntry(zip, "xl/styles.xml", buildStylesXml())
            addZipEntry(zip, "xl/worksheets/sheet1.xml", sheetXml)
        }
    }

    fun exportToBytes(visitas: List<Visit>, lugares: List<Place>): ByteArray {
        val baos = ByteArrayOutputStream()
        exportToStream(visitas, lugares, baos)
        return baos.toByteArray()
    }

    private fun addZipEntry(zip: ZipOutputStream, path: String, content: String) {
        val entry = ZipEntry(path)
        zip.putNextEntry(entry)
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildContentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun buildRootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun buildWorkbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun buildWorkbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="StayTrack Estancias" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    private fun buildStylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><name val="Calibri"/><sz val="11"/></font>
    <font><b/><color rgb="FFFFFFFF"/><name val="Calibri"/><sz val="11"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF0B132B"/></patternFill></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/></border>
  </borders>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
  </cellXfs>
</styleSheet>"""

    private fun buildSheetXml(visitas: List<Visit>, placeMap: Map<String, Place>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("<sheetData>")

        val headers = listOf(
            "ID Estancia", "Lugar", "Categoría", "Latitud", "Longitud",
            "Inicio", "Fin", "Duración (Min)", "Origen", "Precisión (m)"
        )
        sb.append("""<row r="1">""")
        headers.forEachIndexed { colIndex, header ->
            val cellRef = getCellRef(colIndex, 1)
            sb.append("""<c r="$cellRef" s="1" t="inlineStr"><is><t>${escapeXml(header)}</t></is></c>""")
        }
        sb.append("</row>")

        var rowNum = 2
        for (v in visitas) {
            val place = v.placeId?.let { placeMap[it] }
            val placeName = place?.nombre ?: "Sin nombre"
            val category = place?.categoria?.displayName ?: "Otro"
            val startIso = isoFormat.format(Date(v.inicio))
            val endIso = v.fin?.let { isoFormat.format(Date(it)) } ?: "ACTIVA"
            val durationMin = String.format(Locale.US, "%.1f", v.duracionMillis / 60000.0)

            val values = listOf(
                v.id,
                placeName,
                category,
                String.format(Locale.US, "%.6f", v.latitudAncla),
                String.format(Locale.US, "%.6f", v.longitudAncla),
                startIso,
                endIso,
                durationMin,
                v.origen.name,
                String.format(Locale.US, "%.1f", v.precision)
            )

            sb.append("""<row r="$rowNum">""")
            values.forEachIndexed { colIndex, value ->
                val cellRef = getCellRef(colIndex, rowNum)
                sb.append("""<c r="$cellRef" s="0" t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>""")
            }
            sb.append("</row>")
            rowNum++
        }

        sb.append("</sheetData>")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun getCellRef(colIndex: Int, rowIndex: Int): String {
        val colLetter = ('A'.code + colIndex).toChar()
        return "$colLetter$rowIndex"
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
