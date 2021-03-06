package ru.noties.adapt;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static ru.noties.adapt.TestUtils.assertThrows;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AdaptSourceTest {

    @Test
    public void key_provider_returns_hash() {

        final AdaptSource.KeyProvider keyProvider = new AdaptSource.KeyProvider();

        final Class<?>[] types = {
                String.class,
                Integer.class,
                AdaptSourceTest.class
        };

        for (Class<?> type : types) {
            assertEquals(type.hashCode(), keyProvider.provideKey(type));
        }
    }

    @Test
    public void nothing_added_build_throws() {
        assertThrows(
                "AdaptSource.Builder: No entries were added",
                new Runnable() {
                    @Override
                    public void run() {
                        new AdaptSource.Builder<CharSequence>(new AdaptSource.KeyProvider())
                                .build();
                    }
                }
        );
    }

    @Test
    public void attempt_adding_duplicate() {

        final AdaptSource.Builder<CharSequence> builder = new AdaptSource.Builder<>(new AdaptSource.KeyProvider());

        //noinspection unchecked
        assertTrue(builder.append(CharSequence.class, mock(AdaptEntry.class)));

        //noinspection unchecked
        assertFalse(builder.append(CharSequence.class, mock(AdaptEntry.class)));
    }

    @Test
    public void obtain_not_added_throws() {

        final AdaptSource.KeyProvider keyProvider = new AdaptSource.KeyProvider();
        final AdaptSource.Builder<CharSequence> builder = new AdaptSource.Builder<>(keyProvider);
        //noinspection unchecked
        builder.append(CharSequence.class, mock(AdaptEntry.class));

        final AdaptSource<CharSequence> source = builder.build();

        assertThrows(
                "AdaptSource: Specified viewType is not registered with this Adapt instance: " + keyProvider.provideKey(String.class),
                new Runnable() {
                    @Override
                    public void run() {
                        source.entry(keyProvider.provideKey(String.class));
                    }
                }
        );

        assertThrows(
                "AdaptSource: Specified type is not registered with this Adapt instance: " + String.class.getName(),
                new Runnable() {
                    @Override
                    public void run() {
                        source.entry("Not present");
                    }
                }
        );

        assertThrows(
                "AdaptSource: Specified type is not registered with this Adapt instance: " + String.class.getName(),
                new Runnable() {
                    @Override
                    public void run() {
                        source.assignedViewType("Not present");
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        source.assignedViewType(String.class);
                    }
                }
        );
    }

    @Test
    public void regular_obtain() {

        final AdaptSource.KeyProvider keyProvider = new AdaptSource.KeyProvider();
        final AdaptSource.Builder<CharSequence> builder = new AdaptSource.Builder<>(keyProvider);

        //noinspection unchecked
        final AdaptEntry<String> string = mock(AdaptEntry.class);
        //noinspection unchecked
        final AdaptEntry<StringBuilder> stringBuilder = mock(AdaptEntry.class);

        //noinspection EqualsBetweenInconvertibleTypes
        assertTrue(!string.equals(stringBuilder));

        assertTrue(builder.append(String.class, string));
        assertTrue(builder.append(StringBuilder.class, stringBuilder));

        final AdaptSource<CharSequence> source = builder.build();

        assertEquals(string, source.entry(""));
        assertEquals(string, source.entry(keyProvider.provideKey(String.class)));
        assertEquals(keyProvider.provideKey(String.class), source.assignedViewType(""));
        assertEquals(keyProvider.provideKey(String.class), source.assignedViewType(String.class));

        assertEquals(stringBuilder, source.entry(new StringBuilder()));
        assertEquals(stringBuilder, source.entry(keyProvider.provideKey(StringBuilder.class)));
        assertEquals(keyProvider.provideKey(StringBuilder.class), source.assignedViewType(new StringBuilder()));
        assertEquals(keyProvider.provideKey(StringBuilder.class), source.assignedViewType(StringBuilder.class));
    }

    @Test
    public void type_hashcode_is_invalid_type() {

        final AdaptSource.KeyProvider keyProvider = new AdaptSource.KeyProvider() {
            @Override
            int provideKey(@NonNull Class<?> type) {
                return -1;
            }
        };

        final AdaptSource.Builder<CharSequence> builder = new AdaptSource.Builder<>(keyProvider);

        //noinspection unchecked
        builder.append(String.class, mock(AdaptEntry.class));

        assertThrows(
                "AdaptSource.Builder: Specified type: java.lang.String has " +
                        "hashcode value of -1 which is the same value for INVALID_TYPE used by " +
                        "RecyclerView",
                new Runnable() {
                    @Override
                    public void run() {
                        builder.build();
                    }
                }
        );
    }
}