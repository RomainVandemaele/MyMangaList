package be.bf.demo.api

import org.w3c.dom.Document
import java.io.File
import java.io.FileOutputStream
import java.io.StringWriter
import java.net.URL
import java.net.URLConnection
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class MangaAPIToJSON {

    companion object {
        const val BASE_URL = "https://cdn.animenewsnetwork.com/encyclopedia/"
        const val REPORT_ADD_ON = "reports.xml?id=155&type=manga"
        const val API_ADD_ON = "api.xml?manga="

        const val TABLE_NAME = "manga"
        const val COLUMN_NAME_ID = "id"
        const val COLUMN_NAME_NAME = "name"
        const val COLUMN_NAME_VOLUME = "Number of tankoubon"
        const val COLUMN_NAME_DATE = "Vintage"
        const val COLUMN_NAME_PLOT = "Plot Summary"
        const val COLUMN_NAME_AUTHORS = "authors"
        const val COLUMN_NAME_THUMBNAIL_URL = "src"

        const val OUTPUT_PATH = "/home/rvdemael/db2.json"
    }

    fun converter() {
        val fout = FileOutputStream(File(OUTPUT_PATH))
        fout.write( "{\n\t\"manga\": [\n".toByteArray() )

        var nskip = 0;
        val nElement = 50;
        var xml : String = ""
        repeat(235) {
            println("repeat")
            xml =  xmlFromUrl(  "$BASE_URL$REPORT_ADD_ON&nskip=$nskip&nlist=$nElement")
            //println(xml)
            val list = mutableListOf<String>()
            list.addAll(xml.split("<id>"))
            list.removeAt(0)
            var urlDetails = StringBuilder("$BASE_URL$API_ADD_ON")
            for (item in list ){
                val i1 = item.indexOf("<")
                val id = item.substring(0,i1)
                //println("id : $id")
                urlDetails.append("/$id")
            }
            Thread.sleep(800)
            //println(urlDetails)
            val xmlDetails =  xmlFromUrl(  urlDetails.toString())
            //println(xmlDetails)
            val mangaList = mutableListOf<String>()
            mangaList.addAll(xmlDetails.split("<manga"))
            mangaList.removeAt(0)
            for((index,manga) in mangaList.withIndex()) {
                //println("MANGA = \n" + manga + "\n")
                var json = xmlDetailsToJSON(manga)

                //if(index == mangaList.lastIndex) json = StringBuilder(json).deleteCharAt(json.lastIndex).toString()

                fout.write(json.toByteArray())
                //println(json)
            }
            Thread.sleep(500)
            nskip+= nElement
        }
        fout.write("\t]\n}".toByteArray())
        fout.close()
    }

    private fun xmlDetailsToJSON(xml: String) : String {
        val json = StringBuilder("\n\t\t{\n")

        json.append( " \""+COLUMN_NAME_ID+"\": "   + getAttribute(xml, COLUMN_NAME_ID) + ",\n")
        json.append( " \""+COLUMN_NAME_NAME+"\": \"" + getAttribute(xml, COLUMN_NAME_NAME) + "\",\n")
        json.append( " \""+ COLUMN_NAME_THUMBNAIL_URL+"\": \"" + getAttribute(xml, COLUMN_NAME_THUMBNAIL_URL,"null") + "\",\n")

        json.append( " \"" + COLUMN_NAME_VOLUME+"\": "+ this.getInfo(xml, COLUMN_NAME_VOLUME,"0") + ",\n")
        json.append( " \"" + COLUMN_NAME_DATE+"\": \""+ this.getInfo(xml, COLUMN_NAME_DATE,"") + "\",\n")
        json.append( " \"" + COLUMN_NAME_PLOT+"\": \""+ this.getInfo(xml, COLUMN_NAME_PLOT,"") + "\",\n")

        json.append( " \"" + COLUMN_NAME_AUTHORS+"\": ["+ this.getAuthors(xml) + "]\n")


        //json.deleteCharAt(json.lastIndex) //remove ","
        json.append("},\n")
        return json.toString();
    }

    fun getAuthors(src: String) : String {
        var json = java.lang.StringBuilder("");
        val ART = "<task>Art</task>"
        val STORY = "<task>Story</task>"
        val ART_STORY ="<task>Story &amp; Art</task>"

        var i = src.indexOf(ART_STORY)
        if(i!=-1) {
            val author = getInfo(src.substring(i),"person","")
            json.append("{ \"name\":\"").append(author.replace("\""," ")).append("\"},")
        }else {
            val i1 = src.indexOf(ART)
            if(i1!=-1) {
                val author = getInfo(src.substring(i1),"person","")
                json.append("{ \"name\":\"").append(author.replace("\""," ")).append("\"},")
            }
            val i2 = src.indexOf(STORY)
            if(i2!=-1) {
                val author = getInfo(src.substring(i2),"person","")
                json.append("{ \"name\":\"").append(author.replace("\""," ")).append("\"},")
            }
        }
        if(json.isNotBlank()) json.deleteCharAt(json.lastIndex)
        //json.append("]")
        return json.toString()
    }

    fun getInfo(scr: String,columnName: String, default: String) : String {
        var json = java.lang.StringBuilder();
        val i1 = scr.indexOf(columnName)
        if(i1!=-1) {
            val startI = scr.indexOf(">",i1)
            val endI = scr.indexOf("<",startI+1)
            json.append( scr.substring(startI+1 , endI).replace("\""," ") )
        }else {
            json.append(default)
        }
        return json.toString()
    }

    fun getAttribute(scr: String,columnName: String,default: String = "") : String {
        var json = java.lang.StringBuilder();
        var i1 = scr.indexOf(" "+columnName)
        if(i1!=-1) {
            var startI = scr.indexOf("\"",i1)
            var endI = scr.indexOf("\"",startI+1)
            json.append(scr.substring(startI+1 , endI).replace("\""," "))
        }else {
            json.append(default)
        }
        return json.toString();
    }


    private fun xmlFromUrl(urlString: String) : String {

        val url = URL(urlString)
        val conn: URLConnection = url.openConnection()

        val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
        val db: DocumentBuilder = dbf.newDocumentBuilder()
        val doc: Document = db.parse(conn.getInputStream())

        val writer = StringWriter()
        val result: StreamResult = StreamResult(writer)
        val transformerFactory: TransformerFactory = TransformerFactory.newInstance()
        val xform: Transformer = transformerFactory.newTransformer()

        xform.transform(DOMSource(doc), result)
        return writer.toString()
    }
}