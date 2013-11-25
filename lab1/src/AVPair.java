import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that holds an attribute-value pair.
 * 
 * Attributes are "HTTP tokens": a sequence of non-special non-whitespace
 * characters. The special characters are control characters, space, tab and the characters from the following set: <pre>
 * ()[]{}'"<>@,;:\/?=
 * </pre>
 * Values are arbitrary strings and may be missing.
 * @author 
 *
 */
public class AVPair {
	public String attr;
	public String value;
	
	public AVPair(String attr, String value) {
		this.attr = attr;
		this.value = value;
	}

	public static final String REGEX_SQUOTED_VALUE_ESC = "'(((.*?)[^\\\\])??)'";
	public static final String REGEX_DQUOTED_VALUE_ESC = REGEX_SQUOTED_VALUE_ESC.replaceAll("'", "\"");
	public static final String REGEX_TOKEN = "[^()<>@,;:\\\\\"/\\[\\]?={} \t\\p{Cntrl}]+";
	public static final String REGEX_AVPAIR = "("+REGEX_TOKEN+")(\\s*=\\s*("+REGEX_TOKEN +"|" + REGEX_DQUOTED_VALUE_ESC + "))?";
	public static final String REGEX_AVPAIRS =  "\\s*" + REGEX_AVPAIR+"(\\s*;\\s*"+REGEX_AVPAIR+")*";
	public static final Pattern PATTERN_AVPAIR = Pattern.compile(REGEX_AVPAIR);
	public static final Pattern PATTERN_AVPAIRS = Pattern.compile(REGEX_AVPAIRS);


/**
 * Parse the input into a list of attribute-value pairs.
 * The input should be a valid attribute-value pair list: attr=value; attr=value; attr; attr=value...
 * If a value exists, it must be either an HTTP token (see {@link AVPair}) or a double-quoted string.
 * 
 * If you solved the second part of the bonus question, this  method should return null if the
 * input is not a list of attribute-value pairs with the format specified above.
 * @param input
 * @return
 */
	public static List<AVPair> parseAvPairs(String input) {
		Matcher test = PATTERN_AVPAIRS.matcher(input);
		if (!test.matches())
			return null;
		
		Matcher m = PATTERN_AVPAIR.matcher(input);
		ArrayList<AVPair> list = new ArrayList<AVPair>();
		
		while (m.find()) {
			String attr = m.group(1);
			String value;
			if (m.group(5) != null) {
				// a quoted string, we remove the external quotes and replace escaped quotes
				value = m.group(5).replaceAll("\\\\\"", "\"");
			}
			else {
				// a token, no quotes to remove.
				value = m.group(3);
			}
			
			AVPair pair = new AVPair(attr, value);
			list.add(pair);
		}
		
		return list;
	}
}