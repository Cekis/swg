package swg;

import com.sun.deploy.util.ArrayUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.IntStream;

public class BuildoutFile {
    private List<String> columnNames;
    private List<String> columnTypes;
    private List<BuildoutNode> nodes = new ArrayList<>();;
    private String fileName;

    public String getFileName() {
        return fileName;
    }
    public void setFileName(String name) {
        this.fileName = name;
    }

    public List<BuildoutNode> getNodes() {
        if(nodes == null){
            nodes = new ArrayList<>();
        }
        return nodes;
    }
    public void readFile(File file) {
        try{
            parseFile(file);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    private void parseFile(File file) throws IOException {
        try{
            if(file.getName().endsWith(".iff")){
                FileInputStream fis = null;
                BufferedInputStream bi = null;
                try {
                    byte[] formSize = new byte[4];
                    byte[] dtiiFormSize = new byte[4];
                    byte[] colsSize = new byte[4];
                    byte[] typeSize = new byte[4];
                    byte[] rowsSize = new byte[4];

                    fis = new FileInputStream(file);
                    bi = new BufferedInputStream(fis);

                    bi.skip(4);  // skip form tag
                    bi.read(formSize);  // length of form
                    bi.skip(8);  // skip dtii tag
                    bi.read(dtiiFormSize);  // length of dtiiform
                    bi.skip(8);  // skip cols tag
                    bi.read(colsSize);  // length of column names

                    int buffLength = ByteBuffer.wrap(colsSize).getInt();

                    byte[] colsbuffer = new byte[buffLength];

                    bi.read(colsbuffer);

                    parseColumnNames(colsbuffer);

                    bi.skip(4);  // skip type tag
                    bi.read(typeSize);

                    buffLength = ByteBuffer.wrap(typeSize).getInt();

                    byte[] typesBuffer = new byte[buffLength];

                    bi.read(typesBuffer);
                    parseColumnTypes(typesBuffer);

                    bi.skip(4);  // skip rows tag
                    bi.read(rowsSize);

                    buffLength = ByteBuffer.wrap(rowsSize).getInt();

                    byte[] rowsBuffer = new byte[buffLength];
                    bi.read(rowsBuffer);
                    parseRows(rowsBuffer);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }
            else if(file.getName().endsWith(".tab")) {
                BufferedReader rdr = new BufferedReader(new FileReader(file));
                BuildoutNode node = new BuildoutNode();

                // grab column names
                String line = rdr.readLine();
                columnNames = new ArrayList<>(Arrays.asList(line.split("\t")));

                // grab column types
                line = rdr.readLine();
                columnTypes = new ArrayList<>(Arrays.asList(line.split("\t")));

                line = rdr.readLine();
                while (line != null) {
                    nodes.add(parseRow(line));
                    line = rdr.readLine();
                }
            }
        }
        catch (Exception e) {
            System.err.println("EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void parseNodes(byte[] data) throws IOException {

    }
    private void parseColumnNames(byte[] columnNameBuffer) {
        byte[] cNameBuffer = Arrays.copyOfRange(columnNameBuffer, 4, columnNameBuffer.length);
        columnNames = new ArrayList<>();

        // The names are null delimited so use that to split them.
        String[] names = new String(cNameBuffer).split("\0");
        for(String name : names){
            columnNames.add(name);
        }
    }
    private void parseColumnTypes(byte[] columnTypesBuffer) {
        columnTypes = new ArrayList<>();

        // The names are null delimited so use that to split them.
        String[] types = new String(columnTypesBuffer).split("\0");
        for(String type : types){
            columnTypes.add(type);
        }
    }
    private void parseRows(byte[] rowsBuffer) {
        int rowSize = columnNames.size() * 4;
        
        // get the number of rows (stored in first four bytes)
        int iterations = ByteBuffer.wrap(Arrays.copyOfRange(rowsBuffer, 0,4)).order(ByteOrder.LITTLE_ENDIAN).getInt();
        for(int i = 0; i < iterations; i++){
            nodes.add(parseRow(Arrays.copyOfRange(rowsBuffer, i * rowSize + 4, (i + 1) * rowSize + 4)));
        }
    }
    private BuildoutNode parseRow(byte[] rowBuffer) {
        BuildoutNode node = new BuildoutNode();
        for(int i = 0; i < columnNames.size(); i++){
            String colName = columnNames.get(i);
            String colType = columnTypes.get(i);

            Object value = null;

            switch (colType){
                case "i": // int
                    value = ByteBuffer.wrap(Arrays.copyOfRange(rowBuffer, i*4,(i*4)+4)).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    break;
                case "f": // float
                    value = ByteBuffer.wrap(Arrays.copyOfRange(rowBuffer, i*4,(i*4)+4)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    break;
                case "h": // hex
                    byte[] values = Arrays.copyOfRange(rowBuffer, i * 4, (i * 4) + 4);
                    Object[] reversed = IntStream.rangeClosed(1, values.length)
                            .mapToObj(j -> values[values.length - j])
                            .toArray();
                    value = bytesToHexString(reversed);
                    break;
            }

            node.addValue(colName, value);
        }
        return node;
    }
    private BuildoutNode parseRow(String rowData){
        BuildoutNode node = new BuildoutNode();
        List<String> values = new ArrayList(Arrays.asList(rowData.split("\t", -1)));
        while(values.size() < columnNames.size()){
            values.add("");
        }

        for(int i = 0; i < columnNames.size(); i++){
            node.addValue(columnNames.get(i), values.get(i));
        }

        return node;
    }
    private String bytesToHexString(Object[] bytes){
        StringBuilder sb = new StringBuilder();
        for (Object b : bytes){
            sb.append(String.format("%02x", (byte)b&0xff));
        }
        return sb.toString();
    }

    public void updateTemplateNames(Map<String, String> templates) {
        for (BuildoutNode node : nodes){
            String templateHex = (String) node.getValue("shared_template_crc");
            node.setTemplateName(templates.get("0x" + templateHex));
        }
    }
}
