package massbank_export_api;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = org.openapitools.OpenApiGeneratorApplication.class)
@AutoConfigureMockMvc
@Testcontainers
public class ExportApiControllerTest {

    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
        "postgres:17-alpine"
    );

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetVersion() throws Exception {
        mockMvc.perform(get("/version"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$").value(startsWith("export api ")));
    }

    @Test
    public void testCreateConversionTaskNistMSP() throws Exception {
        String requestBody = "{ \"record_list\": [\"MSBNK-IPB_Halle-PB001341\" , \"MSBNK-IPB_Halle-PB000125\"], \"format\": \"nist_msp\" }";
        String expectedResponse = """
Name: Rutin
Synon: 2-(3,4-dihydroxyphenyl)-5,7-dihydroxy-3-[(2S,3R,4S,5S,6R)-3,4,5-trihydroxy-6-[[(2R,3R,4R,5R,6S)-3,4,5-trihydroxy-6-methyloxan-2-yl]oxymethyl]oxan-2-yl]oxychromen-4-one
DB#: MSBNK-IPB_Halle-PB001341
InChIKey: IKGXIBQEEMLURG-NVPNHPEKSA-N
InChI: InChI=1S/C27H30O16/c1-8-17(32)20(35)22(37)26(40-8)39-7-15-18(33)21(36)23(38)27(42-15)43-25-19(34)16-13(31)5-10(28)6-14(16)41-24(25)9-2-3-11(29)12(30)4-9/h2-6,8,15,17-18,20-23,26-33,35-38H,7H2,1H3/t8-,15+,17-,18+,20+,21-,22+,23+,26+,27-/m0/s1
SMILES: C[C@H]1[C@@H]([C@H]([C@H]([C@@H](O1)OC[C@@H]2[C@H]([C@@H]([C@H]([C@@H](O2)OC3=C(OC4=CC(=CC(=C4C3=O)O)O)C5=CC(=C(C=C5)O)O)O)O)O)O)O)O
Precursor_type: [M+H]+
Spectrum_type: MS2
Instrument_type: LC-ESI-QTOF
Instrument: API QSTAR Pulsar i
Ion_mode: POSITIVE
Collision_energy: 10 eV
Formula: C27H30O16
MW: 610
ExactMass: 610.15338
Comments: Parent=-1
Splash: splash10-0wmi-0009506000-98ca7f7c8f3072af4481
Num Peaks: 5
147.063 11
303.050 999
449.108 64
465.102 587
611.161 669

Name: Naringenin
Synon: 5,7-dihydroxy-2-(4-hydroxyphenyl)chroman-4-one
DB#: MSBNK-IPB_Halle-PB000125
InChIKey: FTVWIRXFELQLPI-ZDUSSCGKSA-N
InChI: InChI=1S/C15H12O5/c16-9-3-1-8(2-4-9)13-7-12(19)15-11(18)5-10(17)6-14(15)20-13/h1-6,13,16-18H,7H2/t13-/m0/s1
SMILES: C1[C@H](OC2=CC(=CC(=C2C1=O)O)O)C3=CC=C(C=C3)O
Precursor_type: [M+H]+
Spectrum_type: MS2
Instrument_type: LC-ESI-QTOF
Instrument: API QSTAR Pulsar i
Ion_mode: POSITIVE
Collision_energy: 55 eV
Formula: C15H12O5
MW: 272
ExactMass: 272.06847
Comments: Parent=-1
Splash: splash10-0gbc-9800000000-79055ba218dabe14501d
Num Peaks: 29
53.037 10
55.017 70
65.038 111
67.018 165
68.997 238
69.035 194
77.039 59
79.018 32
81.034 12
83.014 46
85.030 8
91.054 774
92.058 15
92.999 10
95.050 91
97.029 90
107.049 63
111.011 37
115.054 28
119.050 538
120.054 13
123.044 23
125.022 17
128.060 19
147.043 58
152.060 30
153.018 999
154.024 43
157.068 9

""";

        mockMvc.perform(post("/convert")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/plain"))
            .andExpect(content().string(expectedResponse));
    }

    @Test
    public void testCreateConversionTaskRikenMSP() throws Exception {
        String requestBody = "{ \"record_list\": [\"MSBNK-IPB_Halle-PB001341\" , \"MSBNK-IPB_Halle-PB000125\"], \"format\": \"riken_msp\" }";
        String expectedResponse = """
NAME: Rutin
PRECURSORMZ:\s
PRECURSORTYPE: [M+H]+
FORMULA: C27H30O16
Ontology: Flavonoid glycosides
INCHIKEY: IKGXIBQEEMLURG-NVPNHPEKSA-N
INCHI: InChI=1S/C27H30O16/c1-8-17(32)20(35)22(37)26(40-8)39-7-15-18(33)21(36)23(38)27(42-15)43-25-19(34)16-13(31)5-10(28)6-14(16)41-24(25)9-2-3-11(29)12(30)4-9/h2-6,8,15,17-18,20-23,26-33,35-38H,7H2,1H3/t8-,15+,17-,18+,20+,21-,22+,23+,26+,27-/m0/s1
SMILES: C[C@H]1[C@@H]([C@H]([C@H]([C@@H](O1)OC[C@@H]2[C@H]([C@@H]([C@H]([C@@H](O2)OC3=C(OC4=CC(=CC(=C4C3=O)O)O)C5=CC(=C(C=C5)O)O)O)O)O)O)O)O
RETENTIONTIME: 0
INSTRUMENTTYPE: LC-ESI-QTOF
INSTRUMENT: API QSTAR Pulsar i
IONMODE: Positive
LINKS: INCHIKEY:IKGXIBQEEMLURG-NVPNHPEKSA-N; KEGG:C05625; PUBCHEM:CID:5280805; COMPTOX:DTXSID3022326
Comment: DB#=MSBNK-IPB_Halle-PB001341; origin=MassBank; IPB_RECORD: 541; Annotation confident structure
Splash: splash10-0wmi-0009506000-98ca7f7c8f3072af4481
Num Peaks: 5
147.063	121.684
303.050	10000.000
449.108	657.368
465.102	5884.210
611.161	6700.000

NAME: Naringenin
PRECURSORMZ:\s
PRECURSORTYPE: [M+H]+
FORMULA: C15H12O5
Ontology: Flavans
INCHIKEY: FTVWIRXFELQLPI-ZDUSSCGKSA-N
INCHI: InChI=1S/C15H12O5/c16-9-3-1-8(2-4-9)13-7-12(19)15-11(18)5-10(17)6-14(15)20-13/h1-6,13,16-18H,7H2/t13-/m0/s1
SMILES: C1[C@H](OC2=CC(=CC(=C2C1=O)O)O)C3=CC=C(C=C3)O
RETENTIONTIME: 0
INSTRUMENTTYPE: LC-ESI-QTOF
INSTRUMENT: API QSTAR Pulsar i
IONMODE: Positive
LINKS: INCHIKEY:FTVWIRXFELQLPI-ZDUSSCGKSA-N; KEGG:C00509; PUBCHEM:CID:439246; COMPTOX:DTXSID1022392
Comment: DB#=MSBNK-IPB_Halle-PB000125; origin=MassBank; IPB_RECORD: 83; Annotation confident structure
Splash: splash10-0gbc-9800000000-79055ba218dabe14501d
Num Peaks: 29
53.037	111.391
55.017	718.038
65.038	1125.168
67.018	1665.237
68.997	2395.872
69.035	1956.044
77.039	605.468
79.018	331.546
81.034	135.272
83.014	471.723
85.030	98.767
91.054	7753.953
92.058	168.025
92.999	114.286
95.050	921.201
97.029	914.232
107.049	644.063
111.011	382.203
115.054	299.384
119.050	5392.656
120.054	146.851
123.044	249.397
125.022	186.626
128.060	202.332
147.043	597.159
152.060	313.053
153.018	10000.000
154.024	440.901
157.068	101.153

""";

        mockMvc.perform(post("/convert")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/plain"))
            .andExpect(content().string(expectedResponse));
    }

    @Test
    public void testCreateConversionTaskMassBank() throws Exception {
        String requestBody = "{ \"record_list\": [\"MSBNK-IPB_Halle-PB001341\" , \"MSBNK-IPB_Halle-PB000125\"], \"format\": \"massbank\" }";

        byte[] zipBytes = mockMvc.perform(post("/convert")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        List<String> fileNames = new ArrayList<>();
        Map<String, byte[]> extractedFiles = new HashMap<>();
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                fileNames.add(entry.getName());
                extractedFiles.put(entry.getName(), zis.readAllBytes());
            }
        }

        assertTrue(fileNames.contains("MSBNK-IPB_Halle-PB001341.txt"));
        assertTrue(fileNames.contains("MSBNK-IPB_Halle-PB000125.txt"));
        assertEquals(2, fileNames.size());

        for (String fileName : fileNames) {
            try (InputStream is = getClass().getResourceAsStream("/MassBank-data-test/IPB_Halle/" + fileName)) {
                assertNotNull(is, "Resource " + fileName + " not found in test resources");
                byte[] expected = is.readAllBytes();
                byte[] actual = extractedFiles.get(fileName);
                assertArrayEquals(expected, actual, "File content mismatch for " + fileName);
            }
        }
    }

    @Test
    public void testGetMetadata() throws Exception {
        String accession = "MSBNK-IPB_Halle-PB001341";
        String expectedResponse = """
    [
        {
            "@context": "https://schema.org",
            "@type": "Dataset",
            "http://purl.org/dc/terms/conformsTo": {
                "@type": "CreativeWork",
                "@id": "https://bioschemas.org/profiles/Dataset/1.0-RELEASE"
            },
            "@id": "https://massbank.eu/MassBank/RecordDisplay?id=MSBNK-IPB_Halle-PB001341#Dataset",
            "description": "This MassBank record with Accession MSBNK-IPB_Halle-PB001341 contains the MS2 mass spectrum of Rutin with the InChIkey IKGXIBQEEMLURG-NVPNHPEKSA-N.",
            "identifier": "MSBNK-IPB_Halle-PB001341",
            "name": "Rutin; LC-ESI-QTOF; MS2; CE:10 eV; [M+H]+",
            "keywords": [
                {
                    "@type": "DefinedTerm",
                    "name": "Mass spectrometry data",
                    "url": "http://edamontology.org/data_2536",
                    "termCode": "data_2536",
                    "inDefinedTermSet": {
                        "@type": "DefinedTermSet",
                        "name": "Bioinformatics operations, data types, formats, identifiers and topics",
                        "url": "http://edamontology.org"
                    }
                }
            ],
            "license": "https://creativecommons.org/licenses/by-sa/4.0",
            "about": {
                "@type": "ChemicalSubstance",
                "@id": "https://massbank.eu/MassBank/RecordDisplay?id=MSBNK-IPB_Halle-PB001341#ChemicalSubstance"
            },
            "url": "https://massbank.eu/MassBank/RecordDisplay?id=MSBNK-IPB_Halle-PB001341",
            "datePublished": "2016-01-19",
            "citation": "",
            "measurementTechnique": [
                {
                    "@type": "DefinedTerm",
                    "name": "liquid chromatography-mass spectrometry",
                    "url": "http://purl.obolibrary.org/obo/CHMO_0000524",
                    "termCode": "CHMO_0000524",
                    "inDefinedTermSet": {
                        "@type": "DefinedTermSet",
                        "name": "Chemical Methods Ontology",
                        "url": "http://purl.obolibrary.org/obo/chmo.owl"
                    }
                }
            ],
            "includedInDataCatalog": {
                "@type": "DataCatalog",
                "name": "MassBank",
                "url": "https://massbank.eu"
            }
        },
        {
            "@context": "https://schema.org",
            "@type": "ChemicalSubstance",
            "http://purl.org/dc/terms/conformsTo": {
                "@type": "CreativeWork",
                "@id": "https://bioschemas.org/profiles/ChemicalSubstance/0.4-RELEASE"
            },
            "@id": "https://massbank.eu/MassBank/RecordDisplay?id=MSBNK-IPB_Halle-PB001341#ChemicalSubstance",
            "identifier": "MSBNK-IPB_Halle-PB001341",
            "name": "Rutin",
            "url": "https://massbank.eu/MassBank/RecordDisplay?id=MSBNK-IPB_Halle-PB001341",
            "chemicalComposition": "C27H30O16",
            "alternateName": [
                "Rutin",
                "2-(3,4-dihydroxyphenyl)-5,7-dihydroxy-3-[(2S,3R,4S,5S,6R)-3,4,5-trihydroxy-6-[[(2R,3R,4R,5R,6S)-3,4,5-trihydroxy-6-methyloxan-2-yl]oxymethyl]oxan-2-yl]oxychromen-4-one"
            ],
            "hasBioChemEntityPart": [
                {
                    "@context": "https://schema.org",
                    "@type": "MolecularEntity",
                    "http://purl.org/dc/terms/conformsTo": {
                        "@type": "CreativeWork",
                        "@id": "https://bioschemas.org/profiles/MolecularEntity/0.5-RELEASE"
                    },
                    "@id": "https://massbank.eu/MassBank/RecordDisplay?id=MSBNK-IPB_Halle-PB001341#IKGXIBQEEMLURG-NVPNHPEKSA-N",
                    "identifier": "MSBNK-IPB_Halle-PB001341",
                    "name": "Rutin",
                    "url": "https://massbank.eu/MassBank/RecordDisplay?id=MSBNK-IPB_Halle-PB001341",
                    "inChI": "InChI=1S/C27H30O16/c1-8-17(32)20(35)22(37)26(40-8)39-7-15-18(33)21(36)23(38)27(42-15)43-25-19(34)16-13(31)5-10(28)6-14(16)41-24(25)9-2-3-11(29)12(30)4-9/h2-6,8,15,17-18,20-23,26-33,35-38H,7H2,1H3/t8-,15+,17-,18+,20+,21-,22+,23+,26+,27-/m0/s1",
                    "smiles": "C[C@H]1[C@@H]([C@H]([C@H]([C@@H](O1)OC[C@@H]2[C@H]([C@@H]([C@H]([C@@H](O2)OC3=C(OC4=CC(=CC(=C4C3=O)O)O)C5=CC(=C(C=C5)O)O)O)O)O)O)O)O",
                    "molecularFormula": "C27H30O16",
                    "monoisotopicMolecularWeight": 610.15338,
                    "inChIKey": "IKGXIBQEEMLURG-NVPNHPEKSA-N"
                }
            ],
            "subjectOf": {
                "@type": "Dataset",
                "@id": "https://massbank.eu/MassBank/RecordDisplay?id=MSBNK-IPB_Halle-PB001341#Dataset"
            }
        }
    ]
    """;

        mockMvc.perform(get("/metadata/{accession}", accession))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(content().json(expectedResponse));
    }

    @Test
    public void testGetRawtext() throws Exception {
        String accession = "MSBNK-IPB_Halle-PB001341";
        String expectedResponse = """
ACCESSION: MSBNK-IPB_Halle-PB001341
RECORD_TITLE: Rutin; LC-ESI-QTOF; MS2; CE:10 eV; [M+H]+
DATE: 2016.01.19 (Created 2008.05.22, modified 2013.06.04)
AUTHORS: Boettcher C, Institute of Plant Biochemistry, Halle, Germany
LICENSE: CC BY-SA
COMMENT: IPB_RECORD: 541
COMMENT: CONFIDENCE confident structure
CH$NAME: Rutin
CH$NAME: 2-(3,4-dihydroxyphenyl)-5,7-dihydroxy-3-[(2S,3R,4S,5S,6R)-3,4,5-trihydroxy-6-[[(2R,3R,4R,5R,6S)-3,4,5-trihydroxy-6-methyloxan-2-yl]oxymethyl]oxan-2-yl]oxychromen-4-one
CH$COMPOUND_CLASS: Natural Product; Flavonol
CH$FORMULA: C27H30O16
CH$EXACT_MASS: 610.15338
CH$SMILES: C[C@H]1[C@@H]([C@H]([C@H]([C@@H](O1)OC[C@@H]2[C@H]([C@@H]([C@H]([C@@H](O2)OC3=C(OC4=CC(=CC(=C4C3=O)O)O)C5=CC(=C(C=C5)O)O)O)O)O)O)O)O
CH$IUPAC: InChI=1S/C27H30O16/c1-8-17(32)20(35)22(37)26(40-8)39-7-15-18(33)21(36)23(38)27(42-15)43-25-19(34)16-13(31)5-10(28)6-14(16)41-24(25)9-2-3-11(29)12(30)4-9/h2-6,8,15,17-18,20-23,26-33,35-38H,7H2,1H3/t8-,15+,17-,18+,20+,21-,22+,23+,26+,27-/m0/s1
CH$LINK: INCHIKEY IKGXIBQEEMLURG-NVPNHPEKSA-N
CH$LINK: KEGG C05625
CH$LINK: PUBCHEM CID:5280805
CH$LINK: COMPTOX DTXSID3022326
CH$LINK: ChemOnt CHEMONTID:0001111; Organic compounds; Phenylpropanoids and polyketides; Flavonoids; Flavonoid glycosides
AC$INSTRUMENT: API QSTAR Pulsar i
AC$INSTRUMENT_TYPE: LC-ESI-QTOF
AC$MASS_SPECTROMETRY: MS_TYPE MS2
AC$MASS_SPECTROMETRY: ION_MODE POSITIVE
AC$MASS_SPECTROMETRY: COLLISION_ENERGY 10 eV
AC$MASS_SPECTROMETRY: IONIZATION ESI
MS$FOCUSED_ION: PRECURSOR_TYPE [M+H]+
PK$SPLASH: splash10-0wmi-0009506000-98ca7f7c8f3072af4481
PK$NUM_PEAK: 5
PK$PEAK: m/z int. rel.int.
  147.063 121.684 11
  303.050 10000.000 999
  449.108 657.368 64
  465.102 5884.210 587
  611.161 6700.000 669
//
""";

        mockMvc.perform(get("/rawtext/{accession}", accession))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain"))
                .andExpect(content().string(expectedResponse));
    }

    @Test
    public void testValidation() throws Exception {
        String validRecord = """
ACCESSION: MSBNK-IPB_Halle-PB001341
RECORD_TITLE: Rutin; LC-ESI-QTOF; MS2; CE:10 eV; [M+H]+
DATE: 2016.01.19 (Created 2008.05.22, modified 2013.06.04)
AUTHORS: Boettcher C, Institute of Plant Biochemistry, Halle, Germany
LICENSE: CC BY-SA
COMMENT: IPB_RECORD: 541
COMMENT: CONFIDENCE confident structure
CH$NAME: Rutin
CH$NAME: 2-(3,4-dihydroxyphenyl)-5,7-dihydroxy-3-[(2S,3R,4S,5S,6R)-3,4,5-trihydroxy-6-[[(2R,3R,4R,5R,6S)-3,4,5-trihydroxy-6-methyloxan-2-yl]oxymethyl]oxan-2-yl]oxychromen-4-one
CH$COMPOUND_CLASS: Natural Product; Flavonol
CH$FORMULA: C27H30O16
CH$EXACT_MASS: 610.15338
CH$SMILES: C[C@H]1[C@@H]([C@H]([C@H]([C@@H](O1)OC[C@@H]2[C@H]([C@@H]([C@H]([C@@H](O2)OC3=C(OC4=CC(=CC(=C4C3=O)O)O)C5=CC(=C(C=C5)O)O)O)O)O)O)O)O
CH$IUPAC: InChI=1S/C27H30O16/c1-8-17(32)20(35)22(37)26(40-8)39-7-15-18(33)21(36)23(38)27(42-15)43-25-19(34)16-13(31)5-10(28)6-14(16)41-24(25)9-2-3-11(29)12(30)4-9/h2-6,8,15,17-18,20-23,26-33,35-38H,7H2,1H3/t8-,15+,17-,18+,20+,21-,22+,23+,26+,27-/m0/s1
CH$LINK: INCHIKEY IKGXIBQEEMLURG-NVPNHPEKSA-N
CH$LINK: KEGG C05625
CH$LINK: PUBCHEM CID:5280805
CH$LINK: COMPTOX DTXSID3022326
CH$LINK: ChemOnt CHEMONTID:0001111; Organic compounds; Phenylpropanoids and polyketides; Flavonoids; Flavonoid glycosides
AC$INSTRUMENT: API QSTAR Pulsar i
AC$INSTRUMENT_TYPE: LC-ESI-QTOF
AC$MASS_SPECTROMETRY: MS_TYPE MS2
AC$MASS_SPECTROMETRY: ION_MODE POSITIVE
AC$MASS_SPECTROMETRY: COLLISION_ENERGY 10 eV
AC$MASS_SPECTROMETRY: IONIZATION ESI
MS$FOCUSED_ION: PRECURSOR_TYPE [M+H]+
PK$SPLASH: splash10-0wmi-0009506000-98ca7f7c8f3072af4481
PK$NUM_PEAK: 5
PK$PEAK: m/z int. rel.int.
  147.063 121.684 11
  303.050 10000.000 999
  449.108 657.368 64
  465.102 5884.210 587
  611.161 6700.000 669
//
""";

        String invalidRecord = """
ACCESSION:  MSBNK-IPB_Halle-PB001341
RECORD_TITLE: Rutin; LC-ESI-QTOF; MS2; CE:10 eV; [M+H]+
DATE: 2016.01.19 (Created 2008.05.22, modified 2013.06.04)
AUTHORS: Boettcher C, Institute of Plant Biochemistry, Halle, Germany
LICENSE: CC BY-SA
COMMENT: IPB_RECORD: 541
COMMENT: CONFIDENCE confident structure
CH$NAME: Rutin
CH$NAME: 2-(3,4-dihydroxyphenyl)-5,7-dihydroxy-3-[(2S,3R,4S,5S,6R)-3,4,5-trihydroxy-6-[[(2R,3R,4R,5R,6S)-3,4,5-trihydroxy-6-methyloxan-2-yl]oxymethyl]oxan-2-yl]oxychromen-4-one
CH$COMPOUND_CLASS: Natural Product; Flavonol
CH$FORMULA: C27H30O16
CH$EXACT_MASS: 610.15338
CH$SMILES: C[C@H]1[C@@H]([C@H]([C@H]([C@@H](O1)OC[C@@H]2[C@H]([C@@H]([C@H]([C@@H](O2)OC3=C(OC4=CC(=CC(=C4C3=O)O)O)C5=CC(=C(C=C5)O)O)O)O)O)O)O)O
CH$IUPAC: InChI=1S/C27H30O16/c1-8-17(32)20(35)22(37)26(40-8)39-7-15-18(33)21(36)23(38)27(42-15)43-25-19(34)16-13(31)5-10(28)6-14(16)41-24(25)9-2-3-11(29)12(30)4-9/h2-6,8,15,17-18,20-23,26-33,35-38H,7H2,1H3/t8-,15+,17-,18+,20+,21-,22+,23+,26+,27-/m0/s1
CH$LINK: INCHIKEY IKGXIBQEEMLURG-NVPNHPEKSA-N
CH$LINK: KEGG C05625
CH$LINK: PUBCHEM CID:5280805
CH$LINK: COMPTOX DTXSID3022326
CH$LINK: ChemOnt CHEMONTID:0001111; Organic compounds; Phenylpropanoids and polyketides; Flavonoids; Flavonoid glycosides
AC$INSTRUMENT: API QSTAR Pulsar i
AC$INSTRUMENT_TYPE: LC-ESI-QTOF
AC$MASS_SPECTROMETRY: MS_TYPE MS2
AC$MASS_SPECTROMETRY: ION_MODE POSITIVE
AC$MASS_SPECTROMETRY: COLLISION_ENERGY 10 eV
AC$MASS_SPECTROMETRY: IONIZATION ESI
MS$FOCUSED_ION: PRECURSOR_TYPE [M+H]+
PK$SPLASH: splash10-0wmi-0009506000-98ca7f7c8f3072af4481
PK$NUM_PEAK: 5
PK$PEAK: m/z int. rel.int.
  147.063 121.684 11
  303.050 10000.000 999
  449.108 657.368 64
  465.102 5884.210 587
  611.161 6700.000 669
//
""";
        mockMvc.perform(post("/validate")
            .contentType("text/plain")
            .content(validRecord))
            .andExpect(status().isOk());

        String expectedResponse = "{ \"message\": \"letter or digit expected\", \"line\":1, \"column\":11 }";
        mockMvc.perform(post("/validate")
                .contentType("text/plain")
                .content(invalidRecord))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentType("application/json"))
            .andExpect(content().json(expectedResponse));
    }

}