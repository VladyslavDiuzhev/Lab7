package server.database.models;

import java.util.Arrays;

public class VehicleModel extends Model{
    public VehicleModel(){
        this.name = "Vehicles";
        this.fields = Arrays.asList(new FieldDB("id", "SERIAL PRIMARY KEY"),
                new FieldDB("name","varchar(256)"),
                new FieldDB("y", "float8"),
                new FieldDB("x", "int"),
                new FieldDB("creation_date", "TIMESTAMP WITH TIME ZONE"),
                new FieldDB("engine_power", "float8"),
                new FieldDB("type_id", "int"),
                new FieldDB("fuel_type_id", "int"));

    }
}
