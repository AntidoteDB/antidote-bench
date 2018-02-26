package adbm.util.helpers;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class GeneralUtil
{

    public static boolean anyEquals(int number, int... numbers) {
        return Arrays.stream(numbers).anyMatch(n -> n == number);
    }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.trim().isEmpty();
    }

    public static <T> boolean anyNullOrPredicate(Stream<T> objects, Predicate<T> predicate) {
        return objects.anyMatch(object -> object == null || predicate.test(object));
    }
}
