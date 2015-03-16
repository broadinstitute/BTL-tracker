package controllers

/**
 * Created by nnovod on 3/10/15.
 */
object MakeEZPass {
	case class bsp(label: String)
	case object inc
	case object graphCalc
	case object prompt
	case object unknown
	case object calc

	// Used for EZPass
	val bspSmplID = bsp("Sample ID")

	val bspStkSmpl = bsp("Stock Sample")
	val bspRootSmpl = bsp("Root Sample(s)")
	val bspSmplKit = bsp("Sample Kit")
	val bspSite = bsp("Site")
	val bspParticipants = bsp("Participant ID(s)")
	val bspCol = bsp("Collection")
	val bspMaterialType = bsp("Material Type")
	val bspVol = bsp("Volume")
	val bspConc = bsp("Conc")
	// Used by report
	val bspTubeID = bsp("Manufacturer Tube Barcode")
	// Used by report
	val bspContainer = bsp("Container")
	// Used by report
	val bspPos = bsp("Position")
	val bspContainerName = bsp("Container Name")

	// Used for EZPass
	val bspCollaboratorParticipant = bsp("Collaborator Participant ID")

	// Used for EZPass
	val bspCollaboratorSample = bsp("Collaborator Sample ID")

	val bspTissueSite = bsp("Tissue Site")

	// Used for EZPass
	val bspGSSRbarcode = bsp("GSSR Barcode")

	val bspGSSRkit = bsp("GSSR Kit ID")
	val bspStockExport = bsp("Stock At Export")

	val ezPassSource = Map("Sample Number" -> inc,
		"Additional Sample Information" -> "SSF-382", // Jira ticket
		"Project Title Description (e.g. MG1655 Jumping Library Dev.)" -> "Description", // Jira ticket summary
		"Sample Tube Barcode" -> graphCalc,
		"Molecular Barcode Sequence" -> graphCalc,
		"Molecular Barcode Name" -> graphCalc,
		"Source Sample GSSR ID" -> bspGSSRbarcode,
		"Collaborator Sample ID" -> bspCollaboratorSample,
		"Individual Name (aka Patient ID, Required for human subject samples)" -> bspCollaboratorParticipant,
		"Library Name (External Collaborator Library ID)" -> bspSmplID,
		"Illumina or 454 Kit Used" -> "Nextera, otherwise Illumina",
		"Sequencing Technology (Illumina/454/TechX Internal Other)" -> "Illumina",
		"Strain" -> unknown,"Sex (for non-human samples only)" -> unknown,
		"Cell Line"	-> unknown,"Tissue Type" -> unknown,
		"Library Type (see dropdown)" -> unknown, "Data Analysis Type (see dropdown)" -> unknown,
		"Reference Sequence" -> unknown,
		"GSSR # of Bait Pool (If submitting a hybrid selection library, please provide GSSR of bait pool used in experiment in order to properly run Hybrid Selection pipeline analyses)" -> unknown,
		"Insert Size Range bp. (i.e the library size without adapters)" -> calc, // Library Size Range bp. - 126
		"Library Size Range bp. (i.e. the insert size plus adapters)" -> prompt,
		"Jump Size (kb) if applicable" -> unknown,
		"Restriction Enzyme if applicable" -> "Later, for RRBS",
		"Molecular barcode Plate ID" -> unknown,
		"Molecular barcode Plate well ID" -> unknown,
		"Total Library Volume (ul)" -> prompt, // Integer, warning if under 20
		"Total Library Concentration (ng/ul)" -> prompt, // Float
		"Single/Double Stranded (S/D)" -> "D",
		"Funding Source" -> "Jira parent ticket",
		"Coverage (# Lanes/Sample)" -> "Jira parent ticket",
		"Approved By" -> unknown,
		"Requested Completion Date" -> "ASAP",
		"Data Submission (Yes, Yes Later, or No)" -> unknown,
		"Additional Assembly and Analysis Information" -> unknown,
		"Pooled" -> "Y")
}
