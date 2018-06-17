package ru.noties.adapt;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AdaptTest {

    public interface Item {
    }

    public static class Item_1 implements Item {
    }

    public static class Item_2 implements Item {

        final int id;

        public Item_2(int id) {
            this.id = id;
        }
    }

    @Test
    public void same_recycler_view_returned_between_calls() {

        //noinspection unchecked
        final Adapt<Item> adapt = Adapt.builder(Item.class)
                .include(Item_1.class, mock(ItemView.class))
                .build();

        final RecyclerView.Adapter<? extends Holder> adapter = adapt.toRecyclerViewAdapter();
        assertTrue(adapter == adapt.toRecyclerViewAdapter());
        assertTrue(adapter == adapt.toRecyclerViewAdapter());
        assertTrue(adapter == adapt.toRecyclerViewAdapter());
    }

    @Test
    public void set_items_triggers_update() {

        final ValueHolder<List<? extends Item>> oldItemsHolder = new ValueHolder<>();
        final ValueHolder<List<? extends Item>> newItemsHolder = new ValueHolder<>();

        final AdaptUpdate<Item> adaptUpdate = new AdaptUpdate<Item>() {
            @Override
            public void updateItems(@NonNull Source<Item> source, @Nullable List<? extends Item> oldItems, @Nullable List<? extends Item> newItems) {
                oldItemsHolder.hold(oldItems);
                newItemsHolder.hold(newItems);
                source.updateItems(newItems);
            }
        };

        //noinspection unchecked
        final Adapt<Item> adapt = Adapt.builder(Item.class)
                .include(Item_1.class, mock(ItemView.class))
                .adaptUpdate(adaptUpdate)
                .build();

        final List<? extends Item> first = Collections.singletonList(new Item_1());

        adapt.setItems(first);

        assertNull(oldItemsHolder.value);
        assertTrue(first == newItemsHolder.value);

        final List<? extends Item> second = Arrays.asList(
                new Item_1(),
                new Item_1()
        );

        adapt.setItems(second);

        assertTrue(first == oldItemsHolder.value);
        assertTrue(second == newItemsHolder.value);
    }

    @Test
    public void empty_adapt_returns_0_size() {

        //noinspection unchecked
        final Adapt<Item> adapt = Adapt.builder(Item.class)
                .include(Item_1.class, mock(ItemView.class))
                .build();

        final Runnable action = new Runnable() {
            @Override
            public void run() {
                assertEquals(0, adapt.getItemCount());
                assertTrue(adapt.isEmpty());
            }
        };

        action.run();

        adapt.setItems(null);
        action.run();

        adapt.setItems(Collections.<Item>emptyList());
        action.run();

        adapt.setItems(new ArrayList<Item>());
        action.run();
    }

    @Test
    public void get_items_returns_unmodifiable_list() {

        //noinspection unchecked
        final Adapt<Item> adapt = Adapt.builder(Item.class)
                .include(Item_1.class, mock(ItemView.class))
                .build();

        adapt.setItems(Arrays.asList(new Item_1(), new Item_1()));

        final List<Action<List<Item>>> actions = Arrays.asList(
                new Action<List<Item>>() {
                    @Override
                    public void apply(List<Item> items) {
                        items.set(0, new Item_1());
                    }
                },
                new Action<List<Item>>() {
                    @Override
                    public void apply(List<Item> items) {
                        items.add(0, new Item_1());
                    }
                },
                new Action<List<Item>>() {
                    @Override
                    public void apply(List<Item> items) {
                        items.remove(0);
                    }
                },
                new Action<List<Item>>() {
                    @Override
                    public void apply(List<Item> items) {
                        items.addAll(new ArrayList<Item>());
                    }
                },
                new Action<List<Item>>() {
                    @Override
                    public void apply(List<Item> items) {
                        items.sort(new Comparator<Item>() {
                            @SuppressWarnings("ComparatorMethodParameterNotUsed")
                            @Override
                            public int compare(Item o1, Item o2) {
                                return 0;
                            }
                        });
                    }
                }
        );

        for (Action<List<Item>> action: actions) {
            try {
                //noinspection unchecked
                action.apply((List<Item>) adapt.getItems());
                assertTrue(false);
            } catch (UnsupportedOperationException e) {
                assertTrue(true);
            }
        }
    }

    @Test
    public void get_items_non_null_even_if_empty() {

        //noinspection unchecked
        final Adapt<Item> adapt = Adapt.builder(Item.class)
                .include(Item_1.class, mock(ItemView.class))
                .build();

        assertNotNull(adapt.getItems());

        adapt.setItems(null);
        assertNotNull(adapt.getItems());
    }

    @Test
    public void get_item_correct() {

        //noinspection unchecked
        final Adapt<Item> adapt = Adapt.builder(Item.class)
                .include(Item_2.class, mock(ItemView.class))
                .build();

        final List<Item> items = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            items.add(new Item_2(i));
        }

        adapt.setItems(items);

        for (int i = 0; i < 10; i++) {
            assertEquals(i, ((Item_2) adapt.getItem(i)).id);
        }
    }

    @Test
    public void assigned_view_type_correct() {

        final AdaptSource.KeyProvider keyProvider = new AdaptSource.KeyProvider();

        //noinspection unchecked
        final Adapt<Item> adapt = Adapt.builder(Item.class)
                .include(Item_1.class, mock(ItemView.class))
                .build();

        assertEquals(keyProvider.provideKey(Item_1.class), adapt.assignedViewType(Item_1.class));
    }

    // we can... maybe there must be a _validator_ abstraction...
//    @Test
//    public void cannot_add_items_with_nulls() {
//
//        //noinspection unchecked
//        final Adapt<Item> adapt = Adapt.builder(Item.class)
//                .include(Item_1.class, mock(ItemView.class))
//                .build();
//
//        try {
//            adapt.setItems(Arrays.asList(
//                    new Item_1(),
//                    null,
//                    null,
//                    null
//            ));
//            assertTrue(false);
//        } catch (AdaptRuntimeError e) {
//            assertTrue(true);
//        }
//    }

    private static class ValueHolder<T> {

        T value;

        void hold(T value) {
            this.value = value;
        }
    }

    private interface Action<T> {
        void apply(T t);
    }
}