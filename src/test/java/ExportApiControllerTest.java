import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = org.openapitools.OpenApiGeneratorApplication.class)
@AutoConfigureMockMvc
public class ExportApiControllerTest {

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
        String requestBody = "{ \"record_list\": [\"MSBNK-IPB_Halle-PB001341\"], \"format\": \"nist_msp\" }";
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

""";

        mockMvc.perform(post("/convert")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/octet-stream"))
            .andExpect(content().string(expectedResponse));
    }

    @Test
    public void testCreateConversionTaskRikenMSP() throws Exception {
        String requestBody = "{ \"record_list\": [\"MSBNK-IPB_Halle-PB001341\"], \"format\": \"riken_msp\" }";
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

""";

        mockMvc.perform(post("/convert")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/octet-stream"))
            .andExpect(content().string(expectedResponse));
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

}