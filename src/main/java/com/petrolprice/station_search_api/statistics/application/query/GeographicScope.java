package com.petrolprice.station_search_api.statistics.application.query;

public record GeographicScope(String adminArea1Name, String adminArea2Name, String adminArea3Name) {
    public GeographicScope {
        adminArea1Name=clean(adminArea1Name); adminArea2Name=clean(adminArea2Name); adminArea3Name=clean(adminArea3Name);
        if(adminArea2Name!=null&&adminArea1Name==null) throw new IllegalArgumentException("adminArea2 requires adminArea1");
        if(adminArea3Name!=null&&(adminArea1Name==null||adminArea2Name==null)) throw new IllegalArgumentException("adminArea3 requires adminArea1 and adminArea2");
    }
    public static GeographicScope country(){return new GeographicScope(null,null,null);}
    public static GeographicScope administrativeHierarchy(String a1,String a2,String a3){return new GeographicScope(a1,a2,a3);}
    private static String clean(String v){return v==null||v.isBlank()?null:v.trim();}
}
