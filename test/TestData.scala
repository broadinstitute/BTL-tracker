import models.Robot.ABTubeToPlate
import models.Transfer
import models.initialContents.InitialContents

/**
 * Created by nnovod on 4/30/15.
 */
object TestData {

	val rackScanHeader = "DATETIME,RACK,TUBE,BARCODE"

	val rackScanS1 =
		"""12/22/2014 14:21:06,XX-11227502,A01,0177159455
12/22/2014 14:21:06,XX-11227502,B01,0177159432
12/22/2014 14:21:06,XX-11227502,C01,0177159431
12/22/2014 14:21:06,XX-11227502,D01,0177159408
12/22/2014 14:21:06,XX-11227502,E01,0177159407
12/22/2014 14:21:06,XX-11227502,F01,0177159384
12/22/2014 14:21:06,XX-11227502,G01,0177156115
12/22/2014 14:21:06,XX-11227502,H01,0177159360
12/22/2014 14:21:06,XX-11227502,A02,0177159454
12/22/2014 14:21:06,XX-11227502,B02,0177159433
12/22/2014 14:21:06,XX-11227502,C02,0177159430
12/22/2014 14:21:06,XX-11227502,D02,0177159409
12/22/2014 14:21:06,XX-11227502,E02,0177159406
12/22/2014 14:21:06,XX-11227502,F02,0177159385
12/22/2014 14:21:06,XX-11227502,G02,0177159382
12/22/2014 14:21:06,XX-11227502,H02,0177159361
12/22/2014 14:21:06,XX-11227502,A03,0177159453
12/22/2014 14:21:06,XX-11227502,B03,0177159434
12/22/2014 14:21:06,XX-11227502,C03,0177159429
12/22/2014 14:21:06,XX-11227502,D03,0177159410
12/22/2014 14:21:06,XX-11227502,E03,0177159405
12/22/2014 14:21:06,XX-11227502,F03,0177159386
12/22/2014 14:21:06,XX-11227502,G03,0177159381
12/22/2014 14:21:06,XX-11227502,H03,0177159362"""

	val rackScanS2 =
		"""12/22/2014 14:21:06,XX-11227502,A04,0177159452
12/22/2014 14:21:06,XX-11227502,B04,0177159435
12/22/2014 14:21:06,XX-11227502,C04,0177159428
12/22/2014 14:21:06,XX-11227502,D04,0177159411
12/22/2014 14:21:06,XX-11227502,E04,0177159404
12/22/2014 14:21:06,XX-11227502,F04,0177159387
12/22/2014 14:21:06,XX-11227502,G04,0177159380
12/22/2014 14:21:06,XX-11227502,H04,0177159363
12/22/2014 14:21:06,XX-11227502,A05,0177159451
12/22/2014 14:21:06,XX-11227502,B05,0177159436
12/22/2014 14:21:06,XX-11227502,C05,0177159427
12/22/2014 14:21:06,XX-11227502,D05,0177159412
12/22/2014 14:21:06,XX-11227502,E05,0177159403
12/22/2014 14:21:06,XX-11227502,F05,0177159388
12/22/2014 14:21:06,XX-11227502,G05,0177159379
12/22/2014 14:21:06,XX-11227502,H05,0177195873
12/22/2014 14:21:06,XX-11227502,A06,0177159450
12/22/2014 14:21:06,XX-11227502,B06,0177159437
12/22/2014 14:21:06,XX-11227502,C06,0177159426
12/22/2014 14:21:06,XX-11227502,D06,0177159413
12/22/2014 14:21:06,XX-11227502,E06,0177159402
12/22/2014 14:21:06,XX-11227502,F06,0177159389
12/22/2014 14:21:06,XX-11227502,G06,0177159378
12/22/2014 14:21:06,XX-11227502,H06,0177195874"""

	val rackScanS3 =
		"""12/22/2014 14:21:06,XX-11227502,A07,0177159449
12/22/2014 14:21:06,XX-11227502,B07,0177159438
12/22/2014 14:21:06,XX-11227502,C07,0177159425
12/22/2014 14:21:06,XX-11227502,D07,0177159414
12/22/2014 14:21:06,XX-11227502,E07,0177159401
12/22/2014 14:21:06,XX-11227502,F07,0177159390
12/22/2014 14:21:06,XX-11227502,G07,0177159377
12/22/2014 14:21:06,XX-11227502,H07,0177195875
12/22/2014 14:21:06,XX-11227502,A08,0177159448
12/22/2014 14:21:06,XX-11227502,B08,0177156117
12/22/2014 14:21:06,XX-11227502,C08,0177159424
12/22/2014 14:21:06,XX-11227502,D08,0177159415
12/22/2014 14:21:06,XX-11227502,E08,0177159400
12/22/2014 14:21:06,XX-11227502,F08,0177159391
12/22/2014 14:21:06,XX-11227502,G08,0177159376
12/22/2014 14:21:06,XX-11227502,H08,0177195863
12/22/2014 14:21:06,XX-11227502,A09,0177159447
12/22/2014 14:21:06,XX-11227502,B09,0177159440
12/22/2014 14:21:06,XX-11227502,C09,0177159423
12/22/2014 14:21:06,XX-11227502,D09,0177159416
12/22/2014 14:21:06,XX-11227502,E09,0177159399
12/22/2014 14:21:06,XX-11227502,F09,0177159392
12/22/2014 14:21:06,XX-11227502,G09,0177159375
12/22/2014 14:21:06,XX-11227502,H09,0177195862"""

	val rackScanS4 =
		"""12/22/2014 14:21:06,XX-11227502,A10,0177159446
12/22/2014 14:21:06,XX-11227502,B10,0177159441
12/22/2014 14:21:06,XX-11227502,C10,0177159422
12/22/2014 14:21:06,XX-11227502,D10,0177159417
12/22/2014 14:21:06,XX-11227502,E10,0177159398
12/22/2014 14:21:06,XX-11227502,F10,0177159393
12/22/2014 14:21:06,XX-11227502,G10,0177159374
12/22/2014 14:21:06,XX-11227502,H10,0177195861
12/22/2014 14:21:06,XX-11227502,A11,0177159445
12/22/2014 14:21:06,XX-11227502,B11,0177159442
12/22/2014 14:21:06,XX-11227502,C11,0177159421
12/22/2014 14:21:06,XX-11227502,D11,0177159418
12/22/2014 14:21:06,XX-11227502,E11,0177159397
12/22/2014 14:21:06,XX-11227502,F11,0177159394
12/22/2014 14:21:06,XX-11227502,G11,0177159373
12/22/2014 14:21:06,XX-11227502,H11,0177195860
12/22/2014 14:21:06,XX-11227502,A12,0177159444
12/22/2014 14:21:06,XX-11227502,B12,0177159443
12/22/2014 14:21:06,XX-11227502,C12,0177159420
12/22/2014 14:21:06,XX-11227502,D12,0177159419
12/22/2014 14:21:06,XX-11227502,E12,0177159396
12/22/2014 14:21:06,XX-11227502,F12,0177159395
12/22/2014 14:21:06,XX-11227502,G12,0177159372
12/22/2014 14:21:06,XX-11227502,H12,0177195859
		"""

	val rackScan = rackScanHeader + '\n' + rackScanS1 + '\n' + rackScanS2 + '\n' + rackScanS3 + '\n' + rackScanS4
	val rackScanSize = 96

	val rackABdata =
		"""12/22/2014 14:21:06,AB-RACK,A01,AB-T1
12/22/2014 14:21:06,AB-RACK,B01,AB-T2
12/22/2014 14:21:06,AB-RACK,C01,AB-T3
"""
	val rackABscan = rackScanHeader + '\n' + rackABdata
	val rackABscanSize = 3
	val abRobotInstructions =
		List((Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me1,"A01","A01","AB-T1")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me3,"B01","B01","AB-T2")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me1,"A01","A02","AB-T1")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me3,"B01","B02","AB-T2")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me1,"A01","A03","AB-T1")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me3,"B01","B03","AB-T2")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me1,"A01","A04","AB-T1")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me3,"B01","B04","AB-T2")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me1,"A01","A05","AB-T1")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K27ac,"C01","B05","AB-T3")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me1,"A01","A06","AB-T1")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K27ac,"C01","B06","AB-T3")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me1,"A01","A07","AB-T1")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K27ac,"C01","B07","AB-T3")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me1,"A01","A08","AB-T1")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K27ac,"C01","B08","AB-T3")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me3,"B01","A09","AB-T2")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K27ac,"C01","B09","AB-T3")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me3,"B01","A10","AB-T2")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K27ac,"C01","B10","AB-T3")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me3,"B01","A11","AB-T2")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K27ac,"C01","B11","AB-T3")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K4me3,"B01","A12","AB-T2")),None),
			(Some(ABTubeToPlate(1.0f,InitialContents.ContentType.ABH3K27ac,"C01","B12","AB-T3")),None),
			(None,Some("No antibody specified for sample tube 0177195859")),
			(None,Some("No antibody specified for sample tube 0177195860")),
			(None,Some("No antibody specified for sample tube 0177195873")),
			(None,Some("No antibody specified for sample tube 0177195874")),
			(None,Some("No antibody specified for sample tube 0177195861")),
			(None,Some("No antibody specified for sample tube 0177195862")),
			(None,Some("No antibody specified for sample tube 0177195863")),
			(None,Some("No antibody specified for sample tube 0177195875")),
			(None,Some("No antibody specified for sample tube 0177159431")),
			(None,Some("No antibody specified for sample tube 0177159408")),
			(None,Some("No antibody specified for sample tube 0177159407")),
			(None,Some("No antibody specified for sample tube 0177159384")),
			(None,Some("No antibody specified for sample tube 0177156115")),
			(None,Some("No antibody specified for sample tube 0177159360")),
			(None,Some("No antibody specified for sample tube 0177159430")),
			(None,Some("No antibody specified for sample tube 0177159409")),
			(None,Some("No antibody specified for sample tube 0177159406")),
			(None,Some("No antibody specified for sample tube 0177159385")),
			(None,Some("No antibody specified for sample tube 0177159382")),
			(None,Some("No antibody specified for sample tube 0177159361")),
			(None,Some("No antibody specified for sample tube 0177159429")),
			(None,Some("No antibody specified for sample tube 0177159410")),
			(None,Some("No antibody specified for sample tube 0177159405")),
			(None,Some("No antibody specified for sample tube 0177159386")),
			(None,Some("No antibody specified for sample tube 0177159381")),
			(None,Some("No antibody specified for sample tube 0177159362")),
			(None,Some("No antibody specified for sample tube 0177159428")),
			(None,Some("No antibody specified for sample tube 0177159411")),
			(None,Some("No antibody specified for sample tube 0177159404")),
			(None,Some("No antibody specified for sample tube 0177159387")),
			(None,Some("No antibody specified for sample tube 0177159380")),
			(None,Some("No antibody specified for sample tube 0177159363")),
			(None,Some("No antibody specified for sample tube 0177159427")),
			(None,Some("No antibody specified for sample tube 0177159412")),
			(None,Some("No antibody specified for sample tube 0177159403")),
			(None,Some("No antibody specified for sample tube 0177159388")),
			(None,Some("No antibody specified for sample tube 0177159379")),
			(None,Some("No antibody specified for sample tube 0177159426")),
			(None,Some("No antibody specified for sample tube 0177159413")),
			(None,Some("No antibody specified for sample tube 0177159402")),
			(None,Some("No antibody specified for sample tube 0177159389")),
			(None,Some("No antibody specified for sample tube 0177159378")),
			(None,Some("No antibody specified for sample tube 0177159425")),
			(None,Some("No antibody specified for sample tube 0177159414")),
			(None,Some("No antibody specified for sample tube 0177159401")),
			(None,Some("No antibody specified for sample tube 0177159390")),
			(None,Some("No antibody specified for sample tube 0177159377")),
			(None,Some("No antibody specified for sample tube 0177159424")),
			(None,Some("No antibody specified for sample tube 0177159415")),
			(None,Some("No antibody specified for sample tube 0177159400")),
			(None,Some("No antibody specified for sample tube 0177159391")),
			(None,Some("No antibody specified for sample tube 0177159376")),
			(None,Some("No antibody specified for sample tube 0177159423")),
			(None,Some("No antibody specified for sample tube 0177159416")),
			(None,Some("No antibody specified for sample tube 0177159399")),
			(None,Some("No antibody specified for sample tube 0177159392")),
			(None,Some("No antibody specified for sample tube 0177159375")),
			(None,Some("No antibody specified for sample tube 0177159422")),
			(None,Some("No antibody specified for sample tube 0177159417")),
			(None,Some("No antibody specified for sample tube 0177159398")),
			(None,Some("No antibody specified for sample tube 0177159393")),
			(None,Some("No antibody specified for sample tube 0177159374")),
			(None,Some("No antibody specified for sample tube 0177159421")),
			(None,Some("No antibody specified for sample tube 0177159418")),
			(None,Some("No antibody specified for sample tube 0177159397")),
			(None,Some("No antibody specified for sample tube 0177159394")),
			(None,Some("No antibody specified for sample tube 0177159373")),
			(None,Some("No antibody specified for sample tube 0177159420")),
			(None,Some("No antibody specified for sample tube 0177159419")),
			(None,Some("No antibody specified for sample tube 0177159396")),
			(None,Some("No antibody specified for sample tube 0177159395")),
			(None,Some("No antibody specified for sample tube 0177159372")))

	val abTransfers =
		List(Transfer("AB-T1","AB-PLATE",None,None,None,Some(Transfer.Slice.CP),Some(List(0, 1, 2, 3, 4, 5, 6, 7)),true),
			Transfer("AB-T3","AB-PLATE",None,None,None,Some(Transfer.Slice.CP),Some(List(16, 17, 18, 19, 20, 21, 22, 23)),true),
			Transfer("AB-T2","AB-PLATE",None,None,None,Some(Transfer.Slice.CP),Some(List(12, 13, 14, 15, 8, 9, 10, 11)),true))

	val bspDataHeader =
		"""Sample ID	Stock Sample	Participant ID(s)	Collection	Volume	Conc	Manufacturer Tube Barcode	Container	Position	Container Name	Collaborator Participant ID	Collaborator Sample ID	Collaborator Sample ID_2	Country	State	Sample Collection Year	Tissue Site	Antibody	Control Sample"""

	val bspDataRows =
		"""SM-7CUCN	SM-42XIV	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.60875	0177159455	XX-11227502	A01	GSPID_42009_C2_1	UMCG_SZ	9002000001391070	1082134849	Netherlands	Groningen	2012	Stool	H3K4me1	XXX
SM-7CUCO	SM-42X12	PT-12B2Y	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.35926	0177159454	XX-11227502	A02	GSPID_42009_C2_1	9002000001559598	9002000001559598		Netherlands	Groningen	2012	Stool	H3K4me1	XXX
SM-7CUCP	SM-42X2B	PT-12B48	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.499	0177159453	XX-11227502	A03	GSPID_42009_C2_1	9002000001545313	9002000001545313		Netherlands	Groningen	2012	Stool	H3K4me1	XXX
SM-7CUCQ	SM-42X2D	PT-12B4A	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.48967	0177159452	XX-11227502	A04	GSPID_42009_C2_1	9002000001522730	9002000001522730		Netherlands	Groningen	2012	Stool	H3K4me1	XXX
SM-7CUCR	SM-42X2N	PT-12B4K	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.5476	0177159451	XX-11227502	A05	GSPID_42009_C2_1	9002000001576552	9002000001576552		Netherlands	Groningen	2012	Stool	H3K4me1	XXX
SM-7CUCS	SM-42XGV	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.47852	0177159450	XX-11227502	A06	GSPID_42009_C2_1	UMCG_SZ	9002000001379650	1082134775	Netherlands	Groningen	2012	Stool	H3K4me1	XXX
SM-7CUCT	SM-42XH3	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.83368	0177159449	XX-11227502	A07	GSPID_42009_C2_1	UMCG_SZ	9002000001324530	1082134768	Netherlands	Groningen	2012	Stool	H3K4me1	XXX
SM-7CUCU	SM-42XHV	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	2.01745	0177159448	XX-11227502	A08	GSPID_42009_C2_1	UMCG_SZ	9002000001537690	1082134803	Netherlands	Groningen	2012	Stool	H3K4me1	XXX
SM-7CUCV	SM-42VG6	PT-12AQW	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.77015	0177159447	XX-11227502	A09	GSPID_42009_C2_1	9002000001483515	9002000001483515		Netherlands	Groningen	2012	Stool	H3K4me3	XXX
SM-7CUCW	SM-441KD	PT-12AXW	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.54426	0177159446	XX-11227502	A10	GSPID_42009_C2_1	9002000001250513	9002000001250513		Netherlands	Groningen	2012	Stool	H3K4me3	XXX
SM-7CUCX	SM-42VTP	PT-12AD2	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.33207	0177159445	XX-11227502	A11	GSPID_42009_C2_1	9002000001307240	9002000001307240		Netherlands	Groningen	2012	Stool	H3K4me3	XXX
SM-7CUCY	SM-42VSX	PT-12ACA	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.6535	0177159444	XX-11227502	A12	GSPID_42009_C2_1	9002000001283757	9002000001283757		Netherlands	Groningen	2012	Stool	H3K4me3	XXX
SM-7CUCZ	SM-441RG	PT-12A2M	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.38138	0177159432	XX-11227502	B01	GSPID_42009_C2_1	9002000001364111	9002000001364111		Netherlands	Groningen	2012	Stool	H3K4me3	XXX
SM-7CUD1	SM-441I6	PT-12AVP	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.47209	0177159433	XX-11227502	B02	GSPID_42009_C2_1	9002000001421675	9002000001421675		Netherlands	Groningen	2012	Stool	H3K4me3	XXX
SM-7CUD2	SM-441KG	PT-12AXZ	Genome Biology / Cisca Wijmenga - LLDeep	10.0	2.29692	0177159434	XX-11227502	B03	GSPID_42009_C2_1	9002000001459693	9002000001459693		Netherlands	Groningen	2012	Stool	H3K4me3	XXX
SM-7CUD3	SM-441JQ	PT-12AXA	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.39242	0177159435	XX-11227502	B04	GSPID_42009_C2_1	9002000001462201	9002000001462201		Netherlands	Groningen	2012	Stool	H3K4me3	XXX
SM-7CUD4	SM-441JS	PT-12AXC	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.31105	0177159436	XX-11227502	B05	GSPID_42009_C2_1	9002000001259312	9002000001259312		Netherlands	Groningen	2012	Stool	H3K27ac	XXX
SM-7CUD5	SM-441Q5	PT-12A1B	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.64797	0177159437	XX-11227502	B06	GSPID_42009_C2_1	9002000001296441	9002000001296441		Netherlands	Groningen	2012	Stool	H3K27ac	XXX
SM-7CUD6	SM-42WC9	PT-12B9C	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.36528	0177159438	XX-11227502	B07	GSPID_42009_C2_1	9002000001517041	9002000001517041		Netherlands	Groningen	2012	Stool	H3K27ac	XXX
SM-7CUD7	SM-441QG	PT-12A1M	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.41707	0177156117	XX-11227502	B08	GSPID_42009_C2_1	9002000001321947	9002000001321947		Netherlands	Groningen	2012	Stool	H3K27ac	XXX
SM-7CUD8	SM-42WC6	PT-12B99	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.82719	0177159440	XX-11227502	B09	GSPID_42009_C2_1	9002000001484871	9002000001484871		Netherlands	Groningen	2012	Stool	H3K27ac	XXX
SM-7CUD9	SM-42XZD	PT-12A98	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.53965	0177159441	XX-11227502	B10	GSPID_42009_C2_1	9002000001254174	9002000001254174		Netherlands	Groningen	2012	Stool	H3K27ac	XXX
SM-7CUDA	SM-42WC8	PT-12B9B	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.07342	0177159442	XX-11227502	B11	GSPID_42009_C2_1	9002000001470400	9002000001470400		Netherlands	Groningen	2012	Stool	H3K27ac	XXX
SM-7CUDB	SM-42WBZ	PT-12B93	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.07507	0177159443	XX-11227502	B12	GSPID_42009_C2_1	9002000001523405	9002000001523405		Netherlands	Groningen	2012	Stool	H3K27ac	XXX
SM-7CUDC	SM-441S9	PT-12A3F	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.23383	0177159431	XX-11227502	C01	GSPID_42009_C2_1	9002000001227239	9002000001227239		Netherlands	Groningen	2012	Stool
SM-7CUDD	SM-42VT6	PT-12ACI	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.46718	0177159430	XX-11227502	C02	GSPID_42009_C2_1	9002000001261704	9002000001261704		Netherlands	Groningen	2012	Stool
SM-7CUDE	SM-42VVM	PT-12AEY	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.54472	0177159429	XX-11227502	C03	GSPID_42009_C2_1	9002000001347021	9002000001347021		Netherlands	Groningen	2012	Stool
SM-7CUDF	SM-42XOX	PT-12CCS	Genome Biology / Cisca Wijmenga - LLDeep	10.0	2.41732	0177159428	XX-11227502	C04	GSPID_42009_C2_1	9002000001243260	9002000001243260		Netherlands	Groningen	2012	Stool
SM-7CUDG	SM-42XQD	PT-12CE7	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.61694	0177159427	XX-11227502	C05	GSPID_42009_C2_1	9002000001345635	9002000001345635		Netherlands	Groningen	2012	Stool
SM-7CUDH	SM-42XRB	PT-12CF5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.76377	0177159426	XX-11227502	C06	GSPID_42009_C2_1	9002000001361675	9002000001361675		Netherlands	Groningen	2012	Stool
SM-7CUDI	SM-441IB	PT-12AVU	Genome Biology / Cisca Wijmenga - LLDeep	10.0	3.0015	0177159425	XX-11227502	C07	GSPID_42009_C2_1	9002000001418830	9002000001418830		Netherlands	Groningen	2012	Stool
SM-7CUDJ	SM-42VVF	PT-12AER	Genome Biology / Cisca Wijmenga - LLDeep	10.0	2.08757	0177159424	XX-11227502	C08	GSPID_42009_C2_1	9002000001322390	9002000001322390		Netherlands	Groningen	2012	Stool
SM-7CUDK	SM-42XCH	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.28434	0177159423	XX-11227502	C09	GSPID_42009_C2_1	UMCG_SZ	9002000001427400	1082134710	Netherlands
SM-7CUDL	SM-42XC6	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.92679	0177159422	XX-11227502	C10	GSPID_42009_C2_1	UMCG_SZ	9002000001415020	1082134700	Netherlands
SM-7CUDM	SM-42VUK	PT-12ADW	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.63736	0177159421	XX-11227502	C11	GSPID_42009_C2_1	9002000001326836	9002000001326836		Netherlands	Groningen	2012	Stool
SM-7CUDN	SM-42VPT	PT-12AMU	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.5633	0177159420	XX-11227502	C12	GSPID_42009_C2_1	9002000001388561	9002000001388561		Netherlands	Groningen	2012	Stool
SM-7CUDO	SM-42VOV	PT-12ALW	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.79935	0177159408	XX-11227502	D01	GSPID_42009_C2_1	9002000001419443	9002000001419443		Netherlands	Groningen	2012	Stool
SM-7CUDP	SM-42VOR	PT-12ALS	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.35962	0177159409	XX-11227502	D02	GSPID_42009_C2_1	9002000001438430	9002000001438430		Netherlands	Groningen	2012	Stool
SM-7CUDQ	SM-42VQ5	PT-12AN6	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.62453	0177159410	XX-11227502	D03	GSPID_42009_C2_1	9002000001413397	9002000001413397		Netherlands	Groningen	2012	Stool
SM-7CUDR	SM-42VON	PT-12ALO	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.69931	0177159411	XX-11227502	D04	GSPID_42009_C2_1	9002000001422322	9002000001422322		Netherlands	Groningen	2012	Stool
SM-7CUDS	SM-42X2G	PT-12B4D	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.66859	0177159412	XX-11227502	D05	GSPID_42009_C2_1	9002000001564382	9002000001564382		Netherlands	Groningen	2012	Stool
SM-7CUDT	SM-42X1J	PT-12B3G	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.60306	0177159413	XX-11227502	D06	GSPID_42009_C2_1	9002000001538954	9002000001538954		Netherlands	Groningen	2012	Stool
SM-7CUDU	SM-441JK	PT-12AX4	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.47731	0177159414	XX-11227502	D07	GSPID_42009_C2_1	9002000001255529	9002000001255529		Netherlands	Groningen	2012	Stool
SM-7CUDV	SM-42VGU	PT-12ARL	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.46798	0177159415	XX-11227502	D08	GSPID_42009_C2_1	9002000001444393	9002000001444393		Netherlands	Groningen	2012	Stool
SM-7CUDW	SM-42X1C	PT-12B39	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.58312	0177159416	XX-11227502	D09	GSPID_42009_C2_1	9002000001524672	9002000001524672		Netherlands	Groningen	2012	Stool
SM-7CUDX	SM-42X96	PT-12BDR	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.49414	0177159417	XX-11227502	D10	GSPID_42009_C2_1	9002000001530437	9002000001530437		Netherlands	Groningen	2012	Stool
SM-7CUDY	SM-42XAB	PT-12BEW	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.51023	0177159418	XX-11227502	D11	GSPID_42009_C2_1	9002000001193909	9002000001193909		Netherlands	Groningen	2012	Stool
SM-7CUDZ	SM-441QB	PT-12A1H	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.40062	0177159419	XX-11227502	D12	GSPID_42009_C2_1	9002000001300660	9002000001300660		Netherlands	Groningen	2012	Stool
SM-7CUE1	SM-42XI6	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	2.62242	0177159407	XX-11227502	E01	GSPID_42009_C2_1	UMCG_SZ	9002000001476720	1082134813	Netherlands
SM-7CUE2	SM-42XAF	PT-12BF1	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.32667	0177159406	XX-11227502	E02	GSPID_42009_C2_1	9002000001213019	9002000001213019		Netherlands	Groningen	2012	Stool
SM-7CUE3	SM-42XB9	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.04361	0177159405	XX-11227502	E03	GSPID_42009_C2_1	UMCG_SZ	9002000001391220	1082134667	Netherlands
SM-7CUE4	SM-42XBC	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.22093	0177159404	XX-11227502	E04	GSPID_42009_C2_1	UMCG_SZ	9002000001401450	1082134681	Netherlands
SM-7CUE5	SM-42XBM	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.41527	0177159403	XX-11227502	E05	GSPID_42009_C2_1	UMCG_SZ	9002000001368870	1082134671	Netherlands
SM-7CUE6	SM-42XCO	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.44352	0177159402	XX-11227502	E06	GSPID_42009_C2_1	UMCG_SZ	9002000001402970	1082134717	Netherlands
SM-7CUE7	SM-42VOY	PT-12ALZ	Genome Biology / Cisca Wijmenga - LLDeep	10.0	2.20606	0177159401	XX-11227502	E07	GSPID_42009_C2_1	9002000001437746	9002000001437746		Netherlands	Groningen	2012	Stool
SM-7CUE8	SM-42XD5	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.2075	0177159400	XX-11227502	E08	GSPID_42009_C2_1	UMCG_SZ	9002000001365670	1082134733	Netherlands
SM-7CUE9	SM-42VOO	PT-12ALP	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.09905	0177159399	XX-11227502	E09	GSPID_42009_C2_1	9002000001416675	9002000001416675		Netherlands	Groningen	2012	Stool
SM-7CUEA	SM-42XDC	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.53873	0177159398	XX-11227502	E10	GSPID_42009_C2_1	UMCG_SZ	9002000001377910	1082134740	Netherlands
SM-7CUEB	SM-42VOH	PT-12ALI	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.32108	0177159397	XX-11227502	E11	GSPID_42009_C2_1	9002000001436794	9002000001436794		Netherlands	Groningen	2012	Stool
SM-7CUEC	SM-42VHB	PT-12AS2	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.2621	0177159396	XX-11227502	E12	GSPID_42009_C2_1	9002000001452653	9002000001452653		Netherlands	Groningen	2012	Stool
SM-7CUED	SM-42X95	PT-12BDQ	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.48158	0177159384	XX-11227502	F01	GSPID_42009_C2_1	9002000001501005	9002000001501005		Netherlands	Groningen	2012	Stool
SM-7CUEE	SM-42X9B	PT-12BDW	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.44004	0177159385	XX-11227502	F02	GSPID_42009_C2_1	9002000001476459	9002000001476459		Netherlands	Groningen	2012	Stool
SM-7CUEF	SM-42XGX	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.3586	0177159386	XX-11227502	F03	GSPID_42009_C2_1	UMCG_SZ	9002000001385120	1082134773	Netherlands
SM-7CUEG	SM-42XI9	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	2.0983	0177159387	XX-11227502	F04	GSPID_42009_C2_1	UMCG_SZ	9002000001576820	1082134823	Netherlands
SM-7CUEH	SM-42XWX	PT-12A6S	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.80128	0177159388	XX-11227502	F05	GSPID_42009_C2_1	9002000001207989	9002000001207989		Netherlands	Groningen	2012	Stool
SM-7CUEI	SM-42XX1	PT-12A6V	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.90127	0177159389	XX-11227502	F06	GSPID_42009_C2_1	9002000001214562	9002000001214562		Netherlands	Groningen	2012	Stool
SM-7CUEJ	SM-42XX2	PT-12A6W	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.55354	0177159390	XX-11227502	F07	GSPID_42009_C2_1	9002000001179490	9002000001179490		Netherlands	Groningen	2012	Stool
SM-7CUEK	SM-42XXA	PT-12A75	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.60283	0177159391	XX-11227502	F08	GSPID_42009_C2_1	9002000001214606	9002000001214606		Netherlands	Groningen	2012	Stool
SM-7CUEL	SM-42WBO	PT-12B8R	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.39859	0177159392	XX-11227502	F09	GSPID_42009_C2_1	9002000001486419	9002000001486419		Netherlands	Groningen	2012	Stool
SM-7CUEM	SM-42WBJ	PT-12B8M	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.57052	0177159393	XX-11227502	F10	GSPID_42009_C2_1	9002000001480470	9002000001480470		Netherlands	Groningen	2012	Stool
SM-7CUEN	SM-42VGM	PT-12ARD	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.50971	0177159394	XX-11227502	F11	GSPID_42009_C2_1	9002000001447626	9002000001447626		Netherlands	Groningen	2012	Stool
SM-7CUEO	SM-42WBK	PT-12B8N	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.6519	0177159395	XX-11227502	F12	GSPID_42009_C2_1	9002000001475980	9002000001475980		Netherlands	Groningen	2012	Stool
SM-7CUEP	SM-42X8F	PT-12BD1	Genome Biology / Cisca Wijmenga - LLDeep	10.0	2.02239	0177156115	XX-11227502	G01	GSPID_42009_C2_1	9002000001521670	9002000001521670		Netherlands	Groningen	2012	Stool
SM-7CUEQ	SM-42X1I	PT-12B3F	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.8702	0177159382	XX-11227502	G02	GSPID_42009_C2_1	9002000001561424	9002000001561424		Netherlands	Groningen	2012	Stool
SM-7CUER	SM-42VQ3	PT-12AN4	Genome Biology / Cisca Wijmenga - LLDeep	10.0	2.22202	0177159381	XX-11227502	G03	GSPID_42009_C2_1	9002000001411900	9002000001411900		Netherlands	Groningen	2012	Stool
SM-7CUES	SM-42WZU	PT-12B2R	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.3619	0177159380	XX-11227502	G04	GSPID_42009_C2_1	9002000001551947	9002000001551947		Netherlands	Groningen	2012	Stool
SM-7CUET	SM-42XGU	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.53046	0177159379	XX-11227502	G05	GSPID_42009_C2_1	UMCG_SZ	9002000001294410	1082134776	Netherlands
SM-7CUEU	SM-42XHX	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.78825	0177159378	XX-11227502	G06	GSPID_42009_C2_1	UMCG_SZ	9002000001553020	1082134805	Netherlands
SM-7CUEV	SM-42XIQ	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.6069	0177159377	XX-11227502	G07	GSPID_42009_C2_1	UMCG_SZ	9002000001401140	1082134833	Netherlands
SM-7CUEW	SM-42XI4	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.97858	0177159376	XX-11227502	G08	GSPID_42009_C2_1	UMCG_SZ	9002000001543510	1082134811	Netherlands
SM-7CUEX	SM-42XBO	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.06519	0177159375	XX-11227502	G09	GSPID_42009_C2_1	UMCG_SZ	9002000001390280	1082134682	Netherlands
SM-7CUEY	SM-42XDI	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.48519	0177159374	XX-11227502	G10	GSPID_42009_C2_1	UMCG_SZ	9002000001338180	1082134749	Netherlands
SM-7CUEZ	SM-42XXV	PT-12A7Q	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.45828	0177159373	XX-11227502	G11	GSPID_42009_C2_1	9002000001228915	9002000001228915		Netherlands	Groningen	2012	Stool
SM-7CUF1	SM-42XCB	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.5706	0177159372	XX-11227502	G12	GSPID_42009_C2_1	UMCG_SZ	9002000001435670	1082134695	Netherlands
SM-7CUF2	SM-441ST	PT-12A3Z	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.21764	0177159360	XX-11227502	H01	GSPID_42009_C2_1	9002000001210106	9002000001210106		Netherlands	Groningen	2012	Stool
SM-7CUF3	SM-441SS	PT-12A3Y	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.50534	0177159361	XX-11227502	H02	GSPID_42009_C2_1	9002000001247429	9002000001247429		Netherlands	Groningen	2012	Stool
SM-7CUF4	SM-42XYF	PT-12A8A	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.59989	0177159362	XX-11227502	H03	GSPID_42009_C2_1	9002000001260888	9002000001260888		Netherlands	Groningen	2012	Stool
SM-7CUF5	SM-42XBE	PT-111S5	Genome Biology / Cisca Wijmenga - LLDeep	10.0	1.32299	0177159363	XX-11227502	H04	GSPID_42009_C2_1	UMCG_SZ	9002000001393360	1082134679	Netherlands
SM-7CYIO	SM-65WOL	PT-1F1KT	Genome Biology / Sasha Zhernakova (UMCG) - IBS-CACO	10.0	1.7161	0177195873	XX-11232644	F10	GSPID_42121_C2_1	4050000000451LL	4050000000451LL	Netherlands	Groningen	2012	Stool
SM-7CYIP	SM-65WCJ	PT-1EZX6	Genome Biology / Sasha Zhernakova (UMCG) - IBS-CACO	10.0	1.9147	0177195874	XX-11232644	F11	GSPID_42121_C2_1	4050000000335LL	4050000000335LL	Netherlands	Groningen	2012	Stool
SM-7CYIQ	SM-65WOK	PT-1F1KS	Genome Biology / Sasha Zhernakova (UMCG) - IBS-CACO	10.0	2.21051	0177195875	XX-11232644	F12	GSPID_42121_C2_1	4050000000449LL	4050000000449LL	Netherlands	Groningen	2012	Stool
SM-7CYIR	SM-65WCL	PT-1EZX8	Genome Biology / Sasha Zhernakova (UMCG) - IBS-CACO	10.0	1.49659	0177195863	XX-11232644	G01	GSPID_42121_C2_1	4050000000343LL	4050000000343LL	Netherlands	Groningen	2012	Stool
SM-7CYIS	SM-65WCM	PT-1EZX9	Genome Biology / Sasha Zhernakova (UMCG) - IBS-CACO	10.0	1.8673	0177195862	XX-11232644	G02	GSPID_42121_C2_1	4050000000347LL	4050000000347LL	Netherlands	Groningen	2012	Stool
SM-7CYIT	SM-65WCP	PT-1EZXC	Genome Biology / Sasha Zhernakova (UMCG) - IBS-CACO	10.0	1.81212	0177195861	XX-11232644	G03	GSPID_42121_C2_1	4050000000357LL	4050000000357LL	Netherlands	Groningen	2012	Stool
SM-7CYIU	SM-65WCQ	PT-1EZXD	Genome Biology / Sasha Zhernakova (UMCG) - IBS-CACO	10.0	1.18432	0177195860	XX-11232644	G04	GSPID_42121_C2_1	4050000000361LL	4050000000361LL	Netherlands	Groningen	2012	Stool
SM-7CYIV	SM-65WCT	PT-1EZXG	Genome Biology / Sasha Zhernakova (UMCG) - IBS-CACO	10.0	1.53638	0177195859	XX-11232644	G05	GSPID_42121_C2_1	4050000000379LL	4050000000379LL	Netherlands	Groningen	2012	Stool"""

	val bspData = bspDataHeader + '\n' + bspDataRows
}
