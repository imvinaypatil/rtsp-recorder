package recorder.common.ArrayAccess;

public class ArrayAccess <ArrayClass> {

    private final ArrayClass[] array;

    public ArrayAccess(ArrayClass[] array) {
        this.array = array;
    }

    public <T> T get(int itemIndex, Class<T> itemClass) {
        return arrayItem(itemIndex, itemClass, array);
    }
    
    public <T> T get(int itemIndex, Class<T> itemClass, T defaultValue) {
        return arrayItem(itemIndex, itemClass, array, defaultValue);
    }
    
    public static <ArrayClass, ItemClass> ItemClass arrayItem(int itemIndex, Class<ItemClass> itemClass, ArrayClass[] array) {
        try {
            return (ItemClass) array[itemIndex];
        } catch (RuntimeException e) {
            throw new ArrayAccessException(e);
        }
    }

    public static <ArrayClass, ItemClass> ItemClass arrayItem(int itemIndex, Class<ItemClass> itemClass, ArrayClass[] array, ItemClass defaultValue) {
        if (array != null && array.length > itemIndex) {
            try {
                return (ItemClass) array[itemIndex];
            } catch (RuntimeException e) {
                throw new ArrayAccessException(e);
            }
        }
        return defaultValue;
    }

}
