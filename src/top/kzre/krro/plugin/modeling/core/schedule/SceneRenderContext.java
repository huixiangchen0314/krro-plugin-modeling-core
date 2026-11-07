package top.kzre.krro.plugin.modeling.core.schedule;

public final class SceneRenderContext {

    private final Long activeObjectId;

    private SceneRenderContext(Builder builder) {
        this.activeObjectId = builder.activeObjectId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getActiveObjectId() {
        return activeObjectId;
    }


    public static final class Builder {

        private Long activeObjectId;

        private Builder() {
        }

        public Builder activeObjectId(Long activeObjectId) {
            this.activeObjectId = activeObjectId;
            return this;
        }

        public SceneRenderContext build() {
            return new SceneRenderContext(this);
        }
    }
}