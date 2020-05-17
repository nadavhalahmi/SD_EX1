import java.net.URLEncoder
import java.security.MessageDigest
import java.util.*

class Coder {
    fun SHAsum(convertme: ByteArray) : String{
        val md = MessageDigest.getInstance("SHA-1");
        return byteArray2Hex(md.digest(convertme));
    }

    fun byteArray2Hex(hash: ByteArray) : String{
        val formatter = Formatter();
        for (b in hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    fun binary_encode(str: String): String{
        val format_template = "%02x"
        var res = ""
        for(i in str.indices step 2){
            var c = (str[i]+""+str[i+1]).toInt(16).toChar()
            if(c in '0'..'9' || c in 'a'..'z' || c in 'A'..'Z' || c == '.' || c == '-' || c == '_' || c == '~'){
                res += c
            }
            else
                res += "%"+format_template.format(c.toInt()).toString()
        }
        return res
    }


}