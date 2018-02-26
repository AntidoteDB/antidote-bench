package adbm.util.helpers;

import org.apache.logging.log4j.message.ParameterizedMessage;

public class FormatUtil
{
    public static String format(String text)
    {
        return text;
    }

    public static String format(String text, Object... params)
    {
        return ParameterizedMessage.format(text, params);
    }
}
