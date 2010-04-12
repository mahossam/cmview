package cmview.datasources;
import java.io.File;
import java.io.IOException;

import owl.core.structure.*;
import owl.core.structure.graphs.RIGEnsemble;
import cmview.Start;


/** 
 * A contact map data model based on a structure loaded from a PDB file.
 */
public class PdbFileModel extends Model {

	private String fileName;	// needed to load ensembl graph from all models in the file
	
	/**
	 * Overloaded constructor to load the data.
	 * @throws ModelConstructionError 
	 */
	public PdbFileModel(String fileName, String edgeType, double distCutoff, int minSeqSep, int maxSeqSep) throws ModelConstructionError {
		this.edgeType = edgeType; 
		this.distCutoff = distCutoff;
		this.minSeqSep = minSeqSep;
		this.maxSeqSep = maxSeqSep;
		this.pdb = new PdbfilePdb(fileName);
		this.fileName = fileName;
	}

	public PdbFileModel(Model mod) {
	    super(mod);
	}
	
	/**
	 * Returns a shallow copy of this object.
	 */
	public PdbFileModel copy() {
	    return new PdbFileModel(this);
	}
	
	/**
	 * Loads the chain corresponding to the given chain code identifier and model number.
	 * @param pdbChainCode  pdb chain code of the chain to be loaded
	 * @param modelSerial the model number to be loaded
	 * @throws ModelConstructionError if an error occured while trying to load the structure
	 */
	public void load(String pdbChainCode, int modelSerial) throws ModelConstructionError {
		load(pdbChainCode, modelSerial, false);
	}
	
	
	
	/**
	 * Loads the chain corresponding to the given chain code identifier and model number.
	 * If loadEnsembleGraph is true, the graph in this model will be the average graph of the ensemble of all models
	 * instead of the graph of the specified model only. The Pdb object will still correspond to the given model number.
	 * @param pdbChainCode  pdb chain code of the chain to be loaded
	 * @param modelSerial the model number to be loaded
	 * @param loadEnsembleGraph whether to set the graph in this model to the (weighted) ensemble graph of all models
	 * @throws ModelConstructionError if an error occured while trying to load the structure
	 */
	public void load(String pdbChainCode, int modelSerial, boolean loadEnsembleGraph) throws ModelConstructionError {
		// load PDB file
		try {
			this.pdb.load(pdbChainCode,modelSerial);
			super.checkAndAssignSecondaryStructure();
			if(loadEnsembleGraph == false || this.pdb.getModels().length == 1) {
				this.graph = pdb.getRIGraph(edgeType, distCutoff);
			} else {
				RIGEnsemble e = new RIGEnsemble(edgeType, distCutoff);
				try {
					e.loadFromMultiModelFile(new File(this.fileName));
					this.graph = e.getAverageGraph();
					this.graph.setPdbCode(this.pdb.getPdbCode());
					this.graph.setChainCode(pdbChainCode);
					this.setIsGraphWeighted(true);
				} catch (IOException e1) {
					throw new ModelConstructionError("Error loading ensemble graph: " + e1.getMessage());
				}
			}

			// assign a loadedGraphId to this model
			String name = DEFAULT_LOADEDGRAPHID;
			if (!this.graph.getPdbCode().equals(Pdb.NO_PDB_CODE)) {
				name = this.graph.getPdbCode()+this.graph.getChainCode();
			} 
			if (this.graph.getTargetNum()!=0) {
				name = String.format("T%04d",this.graph.getTargetNum());
			}
			this.loadedGraphID = Start.setLoadedGraphID(name, this);

			super.writeTempPdbFile();
			
			super.filterContacts(minSeqSep, maxSeqSep);
			super.printWarnings(pdbChainCode);
			
		} catch (PdbLoadError e) {
			throw new ModelConstructionError(e.getMessage());
		}
	}
}
