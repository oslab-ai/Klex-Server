
# KlexReports - No XML Design Sample <img src="../../resources/klexreports.svg" alt="KlexReports logo" align="right"/>

Shows how in-memory KlexDesign objects can be created without using the JRXML design template, but only the API directly.

### Main Features in This Sample

[Creating In-Memory KlexDesign Objects Using the API Directly (Without JRXML Report Templates)](#noxmldesign)

## <a name='noxmldesign'>Creating</a> In-Memory KlexDesign Objects Using the API Directly (Without JRXML Report Templates)
<div align="right">Documented by <a href='mailto:shertage@users.sourceforge.net'>Sanda Zaharia</a></div>

**Description / Goal**\
How to create in-memory KlexDesign objects without processing a JRXML file, using the object model of the KlexReports API.

**Since:** 0.4.2

### The `KlexDesign` Object

Standard in reporting applications use report templates to define the layout of generated documents. To start a report generation process a report template is needed. Once created, a report template can be filled with appropriate data and generate output document objects during the report generation.

In KlexReports the report generation relies on creating, compiling and filling report templates.\
Report templates are stored in [KlexDesign](https://klexreports.sourceforge.net/api/net/sf/klexreports/engine/design/KlexDesign.html) objects. Instances of this class are the raw material that the engine uses to generate reports.

During the report generation a report template object is compiled and completed with additional information about the runtime expressions evaluation into a [KlexReport](https://klexreports.sourceforge.net/api/net/sf/klexreports/engine/KlexReport.html) object, then is filled with significant data creating a ready-to-print pixel-perfect [KlexPrint](https://klexreports.sourceforge.net/api/net/sf/klexreports/engine/KlexPrint.html) object.

One can obtain [KlexDesign](https://klexreports.sourceforge.net/api/net/sf/klexreports/engine/design/KlexDesign.html) instances either by parsing the JRXML report template files, or independently, through API calls, where using JRXML files is not a possibility. The main purpose is to make available an in-memory [KlexDesign](https://klexreports.sourceforge.net/api/net/sf/klexreports/engine/design/KlexDesign.html) object with all its properties set, in order to generate a valid report.

Taking a look inside the [KlexDesign](https://klexreports.sourceforge.net/api/net/sf/klexreports/engine/design/KlexDesign.html) class, one can see that it extends [JRBaseReport](https://klexreports.sourceforge.net/api/net/sf/klexreports/engine/base/JRBaseReport.html) and it inherits a series of member properties such as:

- `name`
- `pageWidth`
- `pageHeight`
- `topMargin`
- `leftMargin`
- `bottomMargin`
- `rightMargin`
- `orientation`
- `ignorePagination`
- `sectionType`
- `styles`
- etc...

Therefore, creating a [KlexDesign](https://klexreports.sourceforge.net/api/net/sf/klexreports/engine/design/KlexDesign.html) instance may be accomplished by creating an empty [KlexDesign](https://klexreports.sourceforge.net/api/net/sf/klexreports/engine/design/KlexDesign.html) object by calling the default constructor, and then applying setter methods to its members with relevance in the given report.

### The No XML Design Sample

This sample shows how to create an in-memory [KlexDesign](https://klexreports.sourceforge.net/api/net/sf/klexreports/engine/design/KlexDesign.html) instance. The sample doesn't contain any JRXML template file. The [KlexDesign](https://klexreports.sourceforge.net/api/net/sf/klexreports/engine/design/KlexDesign.html) object is hardcoded in the `src/NoXmlDesignApp.java` file (see the `getKlexDesign()` method).

```
  private static KlexDesign getKlexDesign() throws JRException
  {
    //KlexDesign
    KlexDesign klexDesign = new KlexDesign();
    klexDesign.setName("NoXmlDesignReport");
    klexDesign.setPageWidth(595);
    klexDesign.setPageHeight(842);
    klexDesign.setColumnWidth(515);
    klexDesign.setColumnSpacing(0);
    klexDesign.setLeftMargin(40);
    klexDesign.setRightMargin(40);
    klexDesign.setTopMargin(50);
    klexDesign.setBottomMargin(50);

    //Fonts
    JRDesignStyle normalStyle = new JRDesignStyle();
    normalStyle.setName("Sans_Normal");
    normalStyle.setDefault(true);
    normalStyle.setFontName("DejaVu Sans");
    normalStyle.setFontSize(12f);
    normalStyle.setPdfFontName("Helvetica");
    normalStyle.setPdfEncoding("Cp1252");
    normalStyle.setPdfEmbedded(Boolean.FALSE);
    klexDesign.addStyle(normalStyle);

    JRDesignStyle boldStyle = new JRDesignStyle();
    boldStyle.setName("Sans_Bold");
    boldStyle.setFontName("DejaVu Sans");
    boldStyle.setFontSize(12f);
    boldStyle.setBold(Boolean.TRUE);
    boldStyle.setPdfFontName("Helvetica-Bold");
    boldStyle.setPdfEncoding("Cp1252");
    boldStyle.setPdfEmbedded(Boolean.FALSE);
    klexDesign.addStyle(boldStyle);

    JRDesignStyle italicStyle = new JRDesignStyle();
    italicStyle.setName("Sans_Italic");
    italicStyle.setFontName("DejaVu Sans");
    italicStyle.setFontSize(12f);
    italicStyle.setItalic(Boolean.TRUE);
    italicStyle.setPdfFontName("Helvetica-Oblique");
    italicStyle.setPdfEncoding("Cp1252");
    italicStyle.setPdfEmbedded(Boolean.FALSE);
    klexDesign.addStyle(italicStyle);

    //Parameters
    JRDesignParameter parameter = new JRDesignParameter();
    parameter.setName("ReportTitle");
    parameter.setValueClass(java.lang.String.class);
    klexDesign.addParameter(parameter);

    parameter = new JRDesignParameter();
    parameter.setName("OrderByClause");
    parameter.setValueClass(java.lang.String.class);
    klexDesign.addParameter(parameter);

    //Query
    JRDesignQuery query = new JRDesignQuery();
    query.setText("SELECT * FROM Address $P!{OrderByClause}");
    klexDesign.setQuery(query);

    //Fields
    JRDesignField field = new JRDesignField();
    field.setName("Id");
    field.setValueClass(java.lang.Integer.class);
    klexDesign.addField(field);

    field = new JRDesignField();
    field.setName("FirstName");
    field.setValueClass(java.lang.String.class);
    klexDesign.addField(field);

    field = new JRDesignField();
    field.setName("LastName");
    field.setValueClass(java.lang.String.class);
    klexDesign.addField(field);

    field = new JRDesignField();
    field.setName("Street");
    field.setValueClass(java.lang.String.class);
    klexDesign.addField(field);

    field = new JRDesignField();
    field.setName("City");
    field.setValueClass(java.lang.String.class);
    klexDesign.addField(field);

    //Variables
    JRDesignVariable variable = new JRDesignVariable();
    variable.setName("CityNumber");
    variable.setValueClass(java.lang.Integer.class);
    variable.setResetType(ResetTypeEnum.GROUP);
    JRDesignGroup group = new JRDesignGroup();
    group.setName("CityGroup");
    variable.setResetGroup(group.getName());
    variable.setCalculation(CalculationEnum.SYSTEM);
    variable.setInitialValueExpression(new JRDesignExpression("$V{CityNumber} == null ? 1 : ($V{CityNumber} + 1)"));
    klexDesign.addVariable(variable);

    variable = new JRDesignVariable();
    variable.setName("AllCities");
    variable.setValueClass(java.lang.String.class);
    variable.setResetType(ResetTypeEnum.REPORT);
    variable.setCalculation(CalculationEnum.SYSTEM);
    klexDesign.addVariable(variable);

    //Groups
    group.setMinHeightToStartNewPage(60);
    group.setExpression(new JRDesignExpression("$F{City}"));

    JRDesignBand band = new JRDesignBand();
    band.setHeight(20);
    JRDesignTextField textField = new JRDesignTextField();
    textField.setX(0);
    textField.setY(4);
    textField.setWidth(515);
    textField.setHeight(15);
    textField.setBackcolor(new Color(0xC0, 0xC0, 0xC0));
    textField.setMode(ModeEnum.OPAQUE);
    textField.sethTextAlign(hTextAlignEnum.LEFT);
    textField.setStyle(boldStyle);
    textField.setExpression(new JRDesignExpression("\"  \" + String.valueOf($V{CityNumber}) + \". \" + String.valueOf($F{City})"));
    band.addElement(textField);
    JRDesignLine line = new JRDesignLine();
    line.setX(0);
    line.setY(19);
    line.setWidth(515);
    line.setHeight(0);
    band.addElement(line);
    ((JRDesignSection)group.getGroupHeaderSection()).addBand(band);

    band = new JRDesignBand();
    band.setHeight(20);
    line = new JRDesignLine();
    line.setX(0);
    line.setY(-1);
    line.setWidth(515);
    line.setHeight(0);
    band.addElement(line);
    JRDesignStaticText staticText = new JRDesignStaticText();
    staticText.setX(400);
    staticText.setY(0);
    staticText.setWidth(60);
    staticText.setHeight(15);
    staticText.sethTextAlign(hTextAlignEnum.RIGHT);
    staticText.setStyle(boldStyle);
    staticText.setText("Count : ");
    band.addElement(staticText);
    textField = new JRDesignTextField();
    textField.setX(460);
    textField.setY(0);
    textField.setWidth(30);
    textField.setHeight(15);
    textField.sethTextAlign(hTextAlignEnum.RIGHT);
    textField.setStyle(boldStyle);
    textField.setExpression(new JRDesignExpression("$V{CityGroup_COUNT}"));
    band.addElement(textField);
    ((JRDesignSection)group.getGroupFooterSection()).addBand(band);

    klexDesign.addGroup(group);

    //Title
    band = new JRDesignBand();
    band.setHeight(50);
    line = new JRDesignLine();
    line.setX(0);
    line.setY(0);
    line.setWidth(515);
    line.setHeight(0);
    band.addElement(line);
    textField = new JRDesignTextField();
    textField.setBlankWhenNull(true);
    textField.setX(0);
    textField.setY(10);
    textField.setWidth(515);
    textField.setHeight(30);
    textField.sethTextAlign(hTextAlignEnum.CENTER);
    textField.setStyle(normalStyle);
    textField.setFontSize(22f);
    textField.setExpression(new JRDesignExpression("$P{ReportTitle}"));
    band.addElement(textField);
    klexDesign.setTitle(band);

    //Page header
    band = new JRDesignBand();
    band.setHeight(20);
    JRDesignFrame frame = new JRDesignFrame();
    frame.setX(0);
    frame.setY(5);
    frame.setWidth(515);
    frame.setHeight(15);
    frame.setForecolor(new Color(0x33, 0x33, 0x33));
    frame.setBackcolor(new Color(0x33, 0x33, 0x33));
    frame.setMode(ModeEnum.OPAQUE);
    band.addElement(frame);
    staticText = new JRDesignStaticText();
    staticText.setX(0);
    staticText.setY(0);
    staticText.setWidth(55);
    staticText.setHeight(15);
    staticText.setForecolor(Color.white);
    staticText.setBackcolor(new Color(0x33, 0x33, 0x33));
    staticText.setMode(ModeEnum.OPAQUE);
    staticText.sethTextAlign(hTextAlignEnum.CENTER);
    staticText.setStyle(boldStyle);
    staticText.setText("ID");
    frame.addElement(staticText);
    staticText = new JRDesignStaticText();
    staticText.setX(55);
    staticText.setY(0);
    staticText.setWidth(205);
    staticText.setHeight(15);
    staticText.setForecolor(Color.white);
    staticText.setBackcolor(new Color(0x33, 0x33, 0x33));
    staticText.setMode(ModeEnum.OPAQUE);
    staticText.setStyle(boldStyle);
    staticText.setText("Name");
    frame.addElement(staticText);
    staticText = new JRDesignStaticText();
    staticText.setX(260);
    staticText.setY(0);
    staticText.setWidth(255);
    staticText.setHeight(15);
    staticText.setForecolor(Color.white);
    staticText.setBackcolor(new Color(0x33, 0x33, 0x33));
    staticText.setMode(ModeEnum.OPAQUE);
    staticText.setStyle(boldStyle);
    staticText.setText("Street");
    frame.addElement(staticText);
    klexDesign.setPageHeader(band);

    //Column header
    band = new JRDesignBand();
    klexDesign.setColumnHeader(band);

    //Detail
    band = new JRDesignBand();
    band.setHeight(20);
    textField = new JRDesignTextField();
    textField.setX(0);
    textField.setY(4);
    textField.setWidth(50);
    textField.setHeight(15);
    textField.sethTextAlign(hTextAlignEnum.RIGHT);
    textField.setStyle(normalStyle);
    textField.setExpression(new JRDesignExpression("$F{Id}"));
    band.addElement(textField);
    textField = new JRDesignTextField();
    textField.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT);
    textField.setX(55);
    textField.setY(4);
    textField.setWidth(200);
    textField.setHeight(15);
    textField.setPositionType(PositionTypeEnum.FLOAT);
    textField.setStyle(normalStyle);
    textField.setExpression(new JRDesignExpression("$F{FirstName} + \" \" + $F{LastName}"));
    band.addElement(textField);
    textField = new JRDesignTextField();
    textField.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT);
    textField.setX(260);
    textField.setY(4);
    textField.setWidth(255);
    textField.setHeight(15);
    textField.setPositionType(PositionTypeEnum.FLOAT);
    textField.setStyle(normalStyle);
    textField.setExpression(new JRDesignExpression("$F{Street}"));
    band.addElement(textField);
    line = new JRDesignLine();
    line.setX(0);
    line.setY(19);
    line.setWidth(515);
    line.setHeight(0);
    line.setForecolor(new Color(0x80, 0x80, 0x80));
    line.setPositionType(PositionTypeEnum.FLOAT);
    band.addElement(line);
    ((JRDesignSection)klexDesign.getDetailSection()).addBand(band);

    //Column footer
    band = new JRDesignBand();
    klexDesign.setColumnFooter(band);

    //Page footer
    band = new JRDesignBand();
    klexDesign.setPageFooter(band);

    //Summary
    band = new JRDesignBand();
    klexDesign.setSummary(band);

    return klexDesign;
  }
```

The ant `compile` task only calls the `getKlexDesign()` method to load the [KlexDesign](https://klexreports.sourceforge.net/api/net/sf/klexreports/engine/design/KlexDesign.html) object created above, and compiles it into a `.klex `file, which is further used at report filling time:

```
  public void compile() throws JRException
  {
    long start = System.currentTimeMillis();
    KlexDesign klexDesign = getKlexDesign();
    File file = new File("target/reports/NoXmlDesignReport.klex");
    file.getParentFile().mkdirs();
    KlexCompileManager.compileReportToFile(klexDesign, file.getPath());
    System.err.println("Compile time : " + (System.currentTimeMillis() - start));
  }
```

### Running the Sample

Running the sample requires the Apache Maven library. Make sure that maven is already installed on your system (version 3.6 or later).
In a command prompt/terminal window set the current folder to `demo/samples/noxmldesign` within the KlexReports source project and run the following command:

```
> mvn clean compile exec:exec@all
```

It will generate all supported document types containing the sample report in the `demo/samples/noxmldesign/target/reports` directory.
