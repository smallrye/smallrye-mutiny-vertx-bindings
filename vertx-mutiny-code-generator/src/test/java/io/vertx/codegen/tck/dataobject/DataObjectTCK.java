package io.vertx.codegen.tck.dataobject;

import io.vertx.codegen.annotations.VertxGen;

@VertxGen
public interface DataObjectTCK {

    DataObjectWithValues getDataObjectWithValues();

    void setDataObjectWithValues(DataObjectWithValues dataObject);

    DataObjectWithLists getDataObjectWithLists();

    void setDataObjectWithLists(DataObjectWithLists dataObject);

    DataObjectWithMaps getDataObjectWithMaps();

    void setDataObjectWithMaps(DataObjectWithMaps dataObject);

    void methodWithOnlyJsonObjectConstructorDataObject(DataObjectWithOnlyJsonObjectConstructor dataObject);

    void setDataObjectWithBuffer(DataObjectWithNestedBuffer dataObject);

    void setDataObjectWithListAdders(DataObjectWithListAdders dataObject);

    void setDataObjectWithMapAdders(DataObjectWithMapAdders dataObject);

    void setDataObjectWithRecursion(DataObjectWithRecursion dataObject);

}
