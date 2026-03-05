import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.items.ItemStackHandler;

public class Dummy {
    public static void main(String[] args) {
        AttachmentType.Builder<ItemStackHandler> builder = AttachmentType.serializable(() -> new ItemStackHandler(1));
        System.out.println("Success!");
    }
}
