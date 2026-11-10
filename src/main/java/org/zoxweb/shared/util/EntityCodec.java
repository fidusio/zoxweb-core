package org.zoxweb.shared.util;

public interface EntityCodec extends DataEncoder<NVEntity, EntityCodec.EntityID>, DataDecoder<EntityCodec.EntityID, NVEntity> {

    class EntityID
    implements GetMetaType, GetGlobalID<String> {
        public final Class<?> clazz;
        public final String guid;


        public EntityID(NVEntity nvEntity) {
            clazz = nvEntity.getClass();
            guid = nvEntity.getGUID();
        }

        public EntityID(Class<? extends NVEntity> clazz, String guid) {
            this.clazz = clazz;
            this.guid = guid;
        }


        @Override
        public Class<?> getMetaType() {
            return clazz;
        }

        /**
         * @return the global identifier of the object
         */
        @Override
        public String getGUID() {
            return guid;
        }
    }
}
