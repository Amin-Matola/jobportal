package JavaMysql.databases;


import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.stream.Collectors;

public class Table extends Database{

    public String table;
    String columns  = "(";
    public List __data;

    Table( String table, Map<String, String> ...cols) {

        super.init();

        this.table      = table.length() > 1? table  : getConfig().TABLE;

        if( ! table.isEmpty() && cols.length > 0 && cols[0].size() > 0)
            createTable(table, cols[0]);
    }

    void createTable( String table, Map<String, String> cols) {

        if(table.isEmpty()){
            table = this.table;
        }

        if(table.isEmpty() || cols.size() < 1){
            return;
        }

        cols.forEach((k,v) -> {
            columns += f("%s %s,", k, v);
        });

        columns   = columns.substring(0, columns.length() - 1 ) + ")";

        String query  = f("CREATE TABLE IF NOT EXISTS %s %s;", table, columns);

        this.run(query);
    }

    /* *
     * And now we wanna change table name
     * */
    public void rename( String what ){

       this.run(f("RENAME TABLE %s TO %s", this.table, what));
       this.table = what;
    }

    /* *
     * What if we want to delete this table
     * */
    public boolean delete(String table){
        if(table.isEmpty()){
            table = this.table;
        }

        if( ! table.isEmpty() ){
            this.run(f("DROP TABLE IF EXISTS %s", table));
            return true;
        }
        return false;
    }

    public List readResults(ResultSet source, Object ...data){
            List<Map> results = new ArrayList<>();


            try{
                ResultSetMetaData md = source.getMetaData();

                while(source.next()){
                    Map<String, Object> m = new HashMap();
                    for (int i = 1; i <= md.getColumnCount(); i++){
                        try {
                            m.put(md.getColumnName(i), source.getInt(i));
                        } catch (Exception e){
                            m.put(md.getColumnName(i), source.getString(md.getColumnLabel(i)));
                        }

                    };
                    results.add(m);
                }
            } catch (Exception e){
                System.out.println(e);
            }

            __data   = results;

            return __data;
    }

    /* *
     * Get all the data in this table
     * */
    public List getData(){
        return readResults(this.run(f("SELECT * FROM %s", this.table), true));
    }

    /* *
     * Insert data into the table
     * */
    public void insert(String table, Map<String, Object> data){

        if(table.isEmpty()){
            table  = !this.table.isEmpty()? this.table : getConfig().TABLE;
        }

        if(table.isEmpty() || data.size() < 0 || data instanceof Map != true){
            return;
        }

        String q         = f("INSERT INTO %s(", table);

        String cols      = String.join(",", data.keySet());
        String vals      = String.join(",", quote(new ArrayList(data.values())));


        String query     = f("%s %s ) VALUES( %s );", q, cols, vals);
        run(query);

    }

    public List quote(List data) {
        return (List) data.stream().map(
                (i) -> f("'%s'", String.valueOf(i))
        ).collect(Collectors.toList());
    }

    public String buildTest(Map<String, Object> item){
        String result = "";

        if(item.isEmpty())
            result     = "1 = 1";

        for(Map.Entry<String, Object> iterator : item.entrySet()) {
            if (!result.isEmpty() && !result.isBlank())
                result = f("%s and %s='%s'", result, iterator.getKey(), string(iterator.getValue()));
            else
                result = f("%s='%s'", iterator.getKey(), iterator.getValue());
        }
        return result;
    }

    /* *
     * Get single row as array
     * */
    public Map getOne(Map<String, Object> ...test){


        Map<String, Object>  term   =   test.length > 0? test[0] : new HashMap<>();

        List l = readResults(
                            this.run(
                                    f("SELECT * FROM %s WHERE %s;",
                                            this.table, buildTest(term)),
                                    true)
                    );

        if( !l.isEmpty() )
            return (Map) l.get(0);


        return term;
    }

    /* *
     * What if we want to add a column
     * */
    public void addColumn(String name, String description, String ...after){

        if( name.isEmpty() || name.isBlank() )
            return;
        String pos = after.length < 1? this.lastColumn() : after[0];

        String q      = f(
                    "ALTER TABLE %s ADD COLUMN %s %s %s;",
                            this.table,
                            name,
                            description,
                            pos.length() > 1 ? "after " + pos : ""
                        );
        this.run(q);
    }

    public String lastColumn() {
        String q                = f("SELECT * from %s", this.table);
        ResultSet r             = this.run(q, true);

        try {
            ResultSetMetaData md = r.getMetaData();
            return md.getColumnLabel(md.getColumnCount());
        }catch(Exception e){
            print(e);
        }

        return "";
    }

    public int lastColumnId() {
        String q                = f("SELECT * from %s", this.table);
        ResultSet r             = this.run(q, true);

        try {
            return r.getMetaData().getColumnCount();
        } catch( Exception e) {}
        return 0;
    }

    /* *
     * Rename column
     * */
    public void renameColumn( String from, String to, String type){
        this.run(f("ALTER TABLE %s CHANGE %s %s %s", this.table, from, to, type));
    }

    /* *
     * Remove an existing column
     * */
    public void removeColumn(String name){
        this.run(f("ALTER TABLE %s DROP COLUMN %s", this.table, name));
    }

    /* *
     * And we want to clear the table
     * */
    public void clear(){
        this.run(f("DELETE FROM %s", this.table));
    }
}
