package config.builder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import config.model.CacheBlockCfg;
import config.model.CacheChainMem;
import config.model.CacheChainPT;
import config.model.CacheConfigMem;
import config.model.CacheConfigPT;
import config.model.Configuration;
import config.model.IndexCacheBlockCfg;
import config.model.MainMemCacheConfig;
import config.model.MainMemoryAllocConfig;
import config.model.PageAgingConfig;
import config.model.PageTableConfig;
import config.model.SMPNodeConfig;
import config.model.PageTableConfig.DirectMappedPageTableConfig;
import config.model.PageTableConfig.InverseMappedPageTableConfig;
import constants.CacheCoherencePolicyType;
import constants.CacheEvictionPolicyType;

public class ConfigReaderXML {

	/**
	 * also closes the input stream
	 * 
	 * @param is
	 * @return
	 */
	public static void setConfig(InputStream is) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();

			Element rootNode = builder.parse(is).getDocumentElement();
			Configuration.getInstance().virtualAddrNBits = getElementIntValue(
					rootNode, "virtualAddressNBits", 32);
			Configuration.getInstance().pageSizeNBits = getElementIntValue(
					rootNode, "pageSizeNBits", 10);
			
			//page table
			NodeList nl = rootNode.getElementsByTagName("pageTable");
			Element elem;
			String value;
			if (nl != null && nl.getLength() > 0) {
				elem = (Element) nl.item(0);
				Configuration.getInstance().pageTableConfig.accessTimeUnits = getElementIntValue(elem, "accessTimeUnits", 1);
				if (elem.getAttribute("direct") != null
						&& Boolean.parseBoolean(elem.getAttribute("direct"))) {
					Configuration.getInstance().pageTableConfig
							.setMappingType(PageTableConfig.DIRECT_MAPPED_TYPE);
					DirectMappedPageTableConfig dptcfg = (DirectMappedPageTableConfig) Configuration.getInstance().pageTableConfig
							.getMappingConfig();
					// TODO check offsetsLength to exist
					if (elem.getElementsByTagName("offsetLengths") != null
							&& elem.getElementsByTagName("offsetLengths")
									.getLength() > 0) {
						NodeList offsets = ((Element) elem.getElementsByTagName(
								"offsetLengths").item(0))
								.getElementsByTagName("length");
						if (offsets != null && offsets.getLength() > 0) {
							int[] offsetLengths = new int[offsets.getLength()];
							for (int i = 0; i < offsets.getLength(); i++) {
								value = ((Text) ((Element) offsets.item(i))
										.getFirstChild()).getNodeValue();
								try {
									offsetLengths[i] = Integer.parseInt(value);
								} catch (NumberFormatException e) {
									offsetLengths[i] = 0;
								}
							}
							dptcfg.setOffsetsLength(offsetLengths);
						}
					}
					if (elem.getElementsByTagName("searchMethod") != null
							&& elem.getElementsByTagName("searchMethod")
									.getLength() > 0) {
						value = ((Text) ((Element) elem.getElementsByTagName(
								"searchMethod").item(0)).getFirstChild())
								.getNodeValue();
						dptcfg.setSearchMethodTopDown(value != null
								&& value.equalsIgnoreCase("topdown"));
					}
				} else {
					Configuration.getInstance().pageTableConfig
							.setMappingType(PageTableConfig.INVERSE_MAPPED_TYPE);
					InverseMappedPageTableConfig icfg = (InverseMappedPageTableConfig) Configuration.getInstance().pageTableConfig
							.getMappingConfig();
					icfg.setHashAnchorSizeNBits(getElementIntValue(rootNode,
							"hashAnchorSizeNBits", 0));
				}
			}

			nl = rootNode.getElementsByTagName("smpNodeConfig");
			if (nl != null && nl.getLength() > 0) {
				SMPNodeConfig smpNode;
				for (int i = 0; i < nl.getLength(); i++) {
					smpNode = new SMPNodeConfig();
					setSMPNodeConfig(smpNode, (Element) nl.item(i));
					Configuration.getInstance().smpNodeConfigs.add(smpNode);
				}
			}
			if (Configuration.getInstance().getTotalNumberOfProcs() > 1) {
				Configuration.getInstance().cacheMemCacheCoherencePolicy = getCacheCoherencePolicyFromString(getElementValue(
						rootNode, "memCachesCoherencePolicy", "msi"));

			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e2) {
			e2.printStackTrace();
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static <CC extends CacheConfigPT> void setCacheChainPTConfig(
			CacheChainPT<CC> cacheChain, Element ccNode,
			List<CC> availCaches) {
		NodeList nl3 = ccNode.getElementsByTagName("cacheIndex");
		for (int j = 0; nl3 != null && j < nl3.getLength(); j++) {
			cacheChain.caches.add(availCaches.get(Integer
					.parseInt((((Text) (nl3.item(j).getFirstChild()))
							.getNodeValue()))));
		}
		nl3 = ccNode.getElementsByTagName("exclusiveCache");
		if (nl3 != null && nl3.getLength() > 0) {
			cacheChain.exclusiveCacheCfg = new IndexCacheBlockCfg();
			cacheChain.exclusiveCacheCfg.indexCache = getElementIntValue(
					(Element) nl3.item(0), "cacheIndexInArray", 1);
			if(availCaches.get(cacheChain.exclusiveCacheCfg.indexCache).isDataInstrSeparated()){
				cacheChain.exclusiveCacheCfg.setDataInstrSeparated(true);
				cacheChain.exclusiveCacheCfg.getNumberEntriesNBits()[1] = getElementIntValue(
						(Element) nl3.item(0), "victimBufferNumberEntriesInstrNBits", 1);
			}
			cacheChain.exclusiveCacheCfg.getNumberEntriesNBits()[0] = getElementIntValue(
					(Element) nl3.item(0), "victimBufferNumberEntriesNBits", 1);
			cacheChain.exclusiveCacheCfg.setAccessTimeUnits(getElementIntValue(
					(Element) nl3.item(0), "victimBufferAccessTimeUnits", 1));
		}
		else{
			cacheChain.exclusiveCacheCfg = null;
		}

	}
	
	


	private static void setSMPNodeConfig(SMPNodeConfig smpNode, Element rootNode) {
		smpNode.diskAccessTime = getElementIntValue(rootNode, "diskATU", 1);
		smpNode.name = getElementValue(rootNode, "name", "SMPNode");
		int enValue =  getElementIntValue(rootNode, "remoteDataCacheEntriesNBits", 0);
		if(enValue>0){
			smpNode.remoteDataCache = new CacheBlockCfg();
			int intValue =getElementIntValue(rootNode,
					"remoteDataCacheEntriesInstrNBits", -1);
			smpNode.remoteDataCache.setDataInstrSeparated(intValue!=-1);
			if(smpNode.remoteDataCache.isDataInstrSeparated()){
				smpNode.remoteDataCache.getNumberEntriesNBits()[1] = getElementIntValue(rootNode, "remoteDataCacheEntriesInstrNBits", intValue);
			}
			smpNode.remoteDataCache.getNumberEntriesNBits()[0] = enValue ;
			smpNode.remoteDataCache.setAccessTimeUnits(getElementIntValue(rootNode, "remoteDataCacheAccessTimeUnits", 0));
		}
		else{
			smpNode.remoteDataCache = null;
		}
		// page table configuration
		NodeList nl2 = rootNode.getElementsByTagName("mainMemoryConfig");
		Element elem;
		NodeList nl;
		if (nl2 != null && nl2.getLength() > 0) {
			elem = (Element) nl2.item(0);
			MainMemCacheConfig mainMemCfg = smpNode.mainMemoryConfig;
			mainMemCfg.setAccessTimeUnits(getElementIntValue(elem,
					"accessTimeUnits", 10));
			mainMemCfg.setNumberEntriesNBits(getElementIntValue(elem,
					"numberEntriesNBits", 10));
			mainMemCfg.setBusSize(getElementIntValue(elem, "busSize", 10));
			mainMemCfg
					.setEvictionPolicy(getEvictionPolicyValueFromString(getElementValue(
							elem, "evictionPolicy", "random")));
			nl = elem.getElementsByTagName("pageAgingConfig");

			if (nl != null && nl.getLength() > 0) {
				mainMemCfg.pageAgingConfig = new PageAgingConfig();
				mainMemCfg.pageAgingConfig
						.setPageAgingIncrease(getElementIntValue((Element) nl
								.item(0), "pageAgingIncrease", -1));
				mainMemCfg.pageAgingConfig.setMemRefToBeRun(getElementIntValue(
						(Element) nl.item(0), "memRefToRun", -1));
			}
			else{
				mainMemCfg.pageAgingConfig = null;
			}
			nl = rootNode.getElementsByTagName("memAllocConfig");
			if (nl != null && nl.getLength() > 0) {
				mainMemCfg.mainMemoryAllocConfig = new MainMemoryAllocConfig();
				mainMemCfg.mainMemoryAllocConfig.setMinPFF(getElementIntValue(
						(Element) nl.item(0), "minPFF", -1));
				mainMemCfg.mainMemoryAllocConfig.setMaxPFF(getElementIntValue(
						(Element) nl.item(0), "maxPFF", -1));
				mainMemCfg.mainMemoryAllocConfig
						.setNEvictedNodesToRun(getElementIntValue((Element) nl
								.item(0), "evNodesToRun", -1));
			}
			else{
				mainMemCfg.mainMemoryAllocConfig = null;
			}
		}

		nl = rootNode.getElementsByTagName("ptCacheConfig");
		if (nl != null && nl.getLength() > 0) {
			CacheConfigPT cacheConfigPT;
			for (int i = 0; i < nl.getLength(); i++) {
				cacheConfigPT = new CacheConfigPT();
				setCacheConfigPT(cacheConfigPT, (Element) nl.item(i));
				smpNode.cacheConfigsPT.add(cacheConfigPT);
			}
		}

		nl = rootNode.getElementsByTagName("memCacheConfig");
		if (nl != null && nl.getLength() > 0) {
			CacheConfigMem cacheConfigMem;
			for (int i = 0; i < nl.getLength(); i++) {
				cacheConfigMem = new CacheConfigMem();
				setCacheConfigMem(cacheConfigMem, (Element) nl.item(i));
				smpNode.cacheConfigsMem.add(cacheConfigMem);
			}
		}
		nl = rootNode.getElementsByTagName("cpuCacheConfig");
		NodeList nl3;
		if (nl != null && nl.getLength() > 0) {
			for (int i = 0; i < nl.getLength(); i++) {
				smpNode.cpuCachesToPT.add(new CacheChainPT<CacheConfigPT>());
				smpNode.cpuCachesToMem.add(new CacheChainMem());
				nl2 = ((Element) nl.item(i))
						.getElementsByTagName("cpuToPTCaches");
				if (nl2 != null && nl2.getLength() > 0) {
					setCacheChainPTConfig(smpNode.cpuCachesToPT
							.get(smpNode.cpuCachesToPT.size() - 1),
							(Element) nl2.item(0), smpNode.cacheConfigsPT);

				}
				nl2 = ((Element) nl.item(i))
						.getElementsByTagName("cpuToMemCaches");
				if (nl2 != null && nl2.getLength() > 0) {

					setCacheChainPTConfig(smpNode.cpuCachesToMem
							.get(smpNode.cpuCachesToMem.size() - 1),
							(Element) nl2.item(0), smpNode.cacheConfigsMem);

				}

			}
		}

	}

	private static void setCacheConfigPT(CacheConfigPT cacheConfigPT,
			Element configElem) {
		cacheConfigPT.setName(getElementValue(configElem, "name",
				"no_name_cache"));
		int intValue = getElementIntValue(configElem,
				"numberEntriesInstrNBits", -1);
		NodeList nl = configElem.getElementsByTagName("victimCache");
		if (nl != null && nl.getLength() > 0) {
			cacheConfigPT.victimCacheCfg = new CacheBlockCfg();
		}
		cacheConfigPT.setDataInstrSeparated(intValue != -1);

		cacheConfigPT.getNumberEntriesNBits()[0] = getElementIntValue(
				configElem, "numberEntriesNBits", 4);
		if (cacheConfigPT.isDataInstrSeparated()) {
			cacheConfigPT.getNumberEntriesNBits()[1] = getElementIntValue(
					configElem, "numberEntriesInstrNBits", 4);
		}
		cacheConfigPT
				.setEvictionPolicy(getEvictionPolicyValueFromString(getElementValue(
						configElem, "evictionPolicy", "random")));
		cacheConfigPT.setNumberSetsNBits(getElementIntValue(configElem,
				"numberSetsNBits", 1));
		cacheConfigPT.setAccessTimeUnits(getElementIntValue(configElem,
				"accessTimeUnits", 10));
		if (nl != null && nl.getLength() > 0) {
			Element vcache = (Element) nl.item(0);
			cacheConfigPT.victimCacheCfg.getNumberEntriesNBits()[0] = getElementIntValue(
				vcache, "victimCacheNumberEntriesNBits", 1);
			if(cacheConfigPT.isDataInstrSeparated()){
				cacheConfigPT.victimCacheCfg.getNumberEntriesNBits()[1] = getElementIntValue(
						vcache, "victimCacheNumberEntriesInstrNBits", 1);
			}
			cacheConfigPT.victimCacheCfg.setAccessTimeUnits(getElementIntValue(
					vcache, "victimCacheAccessTimeUnits", 1));
			
		}
		else{
			cacheConfigPT.victimCacheCfg = null;
		}
	}

	private static void setCacheConfigMem(CacheConfigMem cacheConfigMem,
			Element configElement) {
		setCacheConfigPT(cacheConfigMem, configElement);
		cacheConfigMem.getBlockSizeNBits()[0] = getElementIntValue(
				configElement, "blockSizeNBits", 4);

		if (cacheConfigMem.isDataInstrSeparated()) {
			cacheConfigMem.getBlockSizeNBits()[1] = getElementIntValue(
					configElement, "blockSizeInstrNBits", 4);

		}
		cacheConfigMem.setBusSize(getElementIntValue(configElement, "busSize",
				1));
		
		cacheConfigMem.isWriteThrough = Boolean.parseBoolean(getElementValue(configElement, "isWriteThrough", "false"));
		cacheConfigMem.isWriteAllocate = Boolean.parseBoolean(getElementValue(configElement, "isWriteAllocate", "true"));
		
	}

	private static String getElementValue(Element parentElement,
			String elementName, String defValue) {
		NodeList tempList;
		Element tempElement;
		Text tempText;
		tempList = parentElement.getElementsByTagName(elementName);
		if (tempList != null && tempList.getLength() > 0) {
			tempElement = (Element) tempList.item(0);
			tempText = (Text) tempElement.getFirstChild();
			return tempText.getNodeValue();
		}
		return defValue;
	}

	private static int getElementIntValue(Element parentElement,
			String elementName, int defValue) {
		String value = getElementValue(parentElement, elementName, null);
		int intValue;
		try {
			intValue = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			intValue = defValue;
		}
		return intValue;
	}

	// put random if evPolicyAsString == null or not as def
	private static short getEvictionPolicyValueFromString(
			String exPolicyAsString) {
		if (exPolicyAsString == null) {
			return CacheEvictionPolicyType.RANDOM_POLICY;
		}
		if (exPolicyAsString.equalsIgnoreCase("random")) {
			return CacheEvictionPolicyType.RANDOM_POLICY;
		} else if (exPolicyAsString.equalsIgnoreCase("fifo")) {
			return CacheEvictionPolicyType.FIFO_POLICY;
		} else if (exPolicyAsString.equalsIgnoreCase("lfu")) {
			return CacheEvictionPolicyType.LFU_POLICY;
		} else if (exPolicyAsString.equalsIgnoreCase("lru")) {
			return CacheEvictionPolicyType.LRU_POLICY;
		} else if (exPolicyAsString.equalsIgnoreCase("nfu")) {
			return CacheEvictionPolicyType.NFU_POLICY;
		} else if (exPolicyAsString.equalsIgnoreCase("nru")) {
			return CacheEvictionPolicyType.NRU_POLICY;
		}
		return CacheEvictionPolicyType.RANDOM_POLICY;
	}

	private static short getCacheCoherencePolicyFromString(String ccpAsString) {
		if (ccpAsString == null) {
			return CacheCoherencePolicyType.MSI;
		}
		if (ccpAsString.equalsIgnoreCase("msi")) {
			return CacheCoherencePolicyType.MSI;
		}
		if (ccpAsString.equalsIgnoreCase("mosi")) {
			return CacheCoherencePolicyType.MOSI;
		}
		if (ccpAsString.equalsIgnoreCase("mesi")) {
			return CacheCoherencePolicyType.MESI;
		}
		if (ccpAsString.equalsIgnoreCase("moesi")) {
			return CacheCoherencePolicyType.MOESI;
		}
		return CacheCoherencePolicyType.MSI;

	}

}
