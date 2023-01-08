package im.conversations.up;

import com.google.gson.annotations.SerializedName;

public final class Id {

    public static final Id UNIFIED_PUSH_VERSION_1 = new Id(new UnifiedPush(1));

    @SerializedName("unifiedpush")
    public final UnifiedPush unifiedPush;

    public Id(final UnifiedPush unifiedPush) {
        this.unifiedPush = unifiedPush;
    }

    public static final class UnifiedPush {
        public final int version;

        public UnifiedPush(final int version) {
            this.version = version;
        }
    }
}
