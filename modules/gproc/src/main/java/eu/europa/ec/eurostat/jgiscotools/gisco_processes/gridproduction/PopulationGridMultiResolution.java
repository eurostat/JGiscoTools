/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridproduction;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.grid.GridCell;

/**
 * 
 * Derive EU population figures from 1km grid to other coarser resolutions.
 * 
 * @author julien Gaffuri
 *
 */
public class PopulationGridMultiResolution {
	static Logger logger = LogManager.getLogger(PopulationGridMultiResolution.class.getName());

	public static void main(String[] args) {
		logger.info("Start");

		//reFormatGeostatFiles();
		produceMultiResolutionPopGrids();

		logger.info("End");
	}

	private static void produceMultiResolutionPopGrids() {

		for(int year : new int[] { /*2006, 2011, */2018 }) {
			logger.info(year);

			//load 1km data
			StatsHypercube popData = CSV.load(GridsProduction.basePath+"pop_grid/pop_grid_"+year+"_1km_full.csv", "TOT_P");

			//remove unnecessary dimensions
			popData.delete("YEAR");
			popData.delete("CNTR_CODE");
			popData.delete("METHD_CL");
			popData.delete("DATA_SRC");
			if(year==2011) popData.delete("TOT_P_CON_DT");

			for(int resKM : GridsProduction.resKMs) {
				logger.info(resKM);

				//output data, as dictionnary
				HashMap<String,Double> out = new HashMap<>();

				//go through 1km population data
				for(Stat s : popData.stats) {
					//get higher resolution grid cell it belongs to
					String newId = new GridCell( s.dims.get("GRD_ID") ).getUpperCell(resKM*1000).getId();

					//set or update value
					Double val = out.get(newId);
					if(val == null)
						out.put(newId, s.value);
					else
						out.put(newId, val + s.value);
				}

				//output data, as stat hypercube
				StatsHypercube sh = new StatsHypercube("GRD_ID");
				for(Entry<String,Double> e : out.entrySet())
					sh.stats.add( new Stat(e.getValue(), "GRD_ID", e.getKey()) );

				//save
				CSV.save(sh, "TOT_P", GridsProduction.basePath+"pop_grid/pop_grid_"+year+"_"+resKM+"km.csv");
			}
		}

	}



	private static void reFormatGeostatFiles() {

		/*logger.info("2006");
		{
			logger.info("Load data...");
			StatsHypercube popData = CSV.load(GridsProduction.basePath+"pop_grid_1km_geostat_raw/2006.csv", "TOT_P");

			//popData.printInfo(false);
			//Dimension: GRD_ID (1946461 dimension values)
			//Dimension: DATA_SRC (28 dimension values)
			//  [NO, FI, AIT:DK, PT, UK-EAW, AT:SI, DK, FR, AIT:FR, UV:FR, SE, FI:SE, SI, AIT:AT, SE:NO, AIT:NL, AIT:PL, EE, UV, FI:NO, AIT:UK-EAW, UV:PT, AIT, AIT:SI, AT, AIT:EE, PL, NL]
			//Dimension: YEAR (2 dimension values)
			//  [2007, 2006]
			//Dimension: CNTR_CODE (1728 dimension values)
			//  [NL(65):DE(3), NL(249):DE(4), PL(68):CZ(17), DE(1268):CH(40), NL(36):BE(11), FI(3):SE(11), FR(5367):CH(315), CH(13):IT(39), FR(9):ES(107), PT(10):ES(2), AT(6):DE(2), PT(41):ES(94), NL(9):DE(12), AT(18):SI(43), NL(9):DE(14), SK(17):CZ(9), AT(250):CH(786), PL(73):DE(382), NL(11):BE(100), DE(4):CZ(187), NL(4):BE(21), FR(2390):BE(74), NL(4):BE(23), FR(184):CH(165), UK-NIR(158):IE(6), FR(17):BE(980), NL(13):DE(2), DE(76):BE(48), DE(28):CH(6), FR(66):BE(128), FR(2):BE(271), AT(13):SI(18), FR(172):LU(222), NL(3):BE(8), DE(318):CH(68), FR(62):CH(29), DE(262):CZ(256), NL(14):DE(19), UK-NIR(53):IE(29), PL(4):DE(5), NL(727):DE(25), EL(1):BG(13), NL(27):DE(8), CH(152):IT(103), UK-NIR(20):IE(7), DE(93):CZ(950), NL(6):DE(19), NL(24):DE(7), PT(77):ES(35), FR(2):BE(23), AT(15):LI(43), FR(89):BE(5), FR(211):CH(190), PL(3):CZ(13), DE(451):CH(468), CH(47):IT(178), PT(29):ES(61), DE(191):LU(25), FR(179):LU(15), FR(4):CH(14), AT(10):SI(9), FI(1):SE(8), UK-NIR(19):IE(36), AT(100):DE(2), NL(35):DE(8), PL(42):CZ(194), FR(2):BE(22), NL(27):DE(22), SK(762):HU(2), CH(9):LI(527), SK(751):HU(2), NL(363):BE(13), LV(54):LT(50), DE(2915):CH(322), NL(64):DE(16), DE(350):LU(77), PL(21):CZ(3), DE(11):CZ(2), FR(233):CH(5), NL(1):BE(25), NL(28):BE(32), FR(343):BE(484), NL(7):BE(61), PL(592):CZ(1633), LU(17):BE(28), UK-NIR(3):IE(10), SK(118):HU(77), NL(13):DE(7), AT(151):HU(8), DE(551):CZ(521), DE(587):BE(176), DE(321):BE(4), UK-NIR(10):IE(33), FI(1):SE(6), AT(37):DE(144), PL(6):DE(415), FI(4):SE(26), AT(1):CZ(22), CH(87):LI(268), DE(32):CH(195), FI(30):SE(4), AT(1):DE(38), UK-NIR(99):IE(111), FR(100):BE(34), FR(41):ES(149), AT(6):DE(9), NL(37):DE(170), DE(18):BE(2), NL(14):DE(14), PL(34):CZ(692), AT(10):CH(122), PL(10):LT(25), FR(413):CH(178), FR(39):DE(50), UK-SCT, FR(22):ES(8), NL(16):DE(1), NL(13):DE(9), PL(7):DE(9), FR(12):BE(103), FR(200):LU(2072), SE(3):NO(261), FR(2):BE(26), FR(6):BE(30), FR(17):BE(103), CH(46):LI(11), NL(267):DE(2237), FR(223):DE(1073), AT(305):DE(407), PT(6):ES(36), PL(5435):DE(659), PT(54):ES(4), NL(3):DE(4), PL(6982):CZ(243), PL(13):SK(102), NL(13):BE(1), NL(13):BE(2), DE(115):LU(4), DE(49):CZ(847), DE(297):CZ(757), FR(2):BE(24), SE(4):NO(21), AT(7):SI(23), NL(1):BE(223), FR(2):BE(145), AT(10):SI(15), DE(24):BE(17), PT(2):ES(182), LV(545):LT(64), DE(449):CH(2), SI(50):IT(8), NL(3):DE(2), AT(106):DE(710), FR(82):ES(8), PL(965):DE(287), FI(11):SE(7), DE(2020):CH(19), FR(14):CH(711), AT(80):SI(75), PL(3):CZ(2), NL(80):BE(10), NL(37):DE(34), NL(43):DE(2), NL(719):BE(1505), FR(1):ES(61), PL(101):CZ(43), UK-NIR(53):IE(8), NL(14):DE(17), CH(18):LI(324), UK-NIR(268):IE(10), UK-NIR(18):IE(18), FR(12):DE(7), DE(511):BE(1), FR(876):BE(5), AT(11):DE(124), AT(48):DE(47), NL(1867):DE(1305), LV(27):LT(29), SK(17):CZ(8), FR(228):CH(392), CH(10):LI(127), FR(633):DE(57), PL(80):CZ(153), LV(14):LT(3), DE(234):LU(1564), PL(130):CZ(4), DE(7):CH(17), NL(5):BE(38), PL(7):DE(773), NL(279):BE(13), FI(6):SE(18), FR(215):ES(421), UK-NIR(18):IE(16), DE(33):LU(8), FR(172):BE(38), PL(38):CZ(9), FR(107):BE(91), DE(29):CH(10), FI(10):NO(1), PL(2):DE(1427), CH(85):IT(0), LV(241):LT(4), PL(6):CZ(1307), FR(12):BE(1), DE(1204):BE(130), NL(109):BE(87), NL(207):BE(60), FR(708):DE(2), FR(4):DE(518), CH(813):IT(788), AT(892):DE(726), FI(55):SE(316), NL(33):DE(16), PT(172):ES(2), DE(354):BE(70), NL(42):BE(12), FR(72):BE(20), PL(436):CZ(27), FR(365):DE(350), NL(787):BE(4), PL(1):LT(9), DE(65):LU(400), FR(1):BE(15), FR(25):BE(21), CH(52):IT(1182), AT(2):SI(7), DE(4):CZ(47), PL(5):LT(6), AT(453):DE(389), FR(18):BE(13), NL(6):DE(3), UK-NIR(33):IE(16), PL(214):CZ(956), SE(9):NO(4), FR(2):CH(24), UK-NIR(5):IE(47), AT(660):SK(136), DE(1):LU(9), FR(20):DE(408), DE(11):LU(4), DE(11):CH(11), NL(6):DE(61), DE(4):LU(99), NL(6):DE(4), FR(1296):BE(1523), FR(381):DE(2), DK(17):DE(4), NL(17):BE(17), FR(990):BE(1509), FR(122):BE(8), DE(7):CZ(3), CH(179):LI(24), DE(81):CZ(26), NL(20):BE(6), AT(1155):DE(6), FR(171):ES(21), DE(373):LU(95), NL(6):BE(2), AT(24):SI(5), UK-NIR(1):IE(60), EE(3):LV(8), NL(188):BE(220), DE(6):CZ(30), CH(39):LI(1102), FR(805):BE(507), FR(41):BE(3), UK-NIR(28):IE(26), PT(4):ES(92), DE(7):CZ(1), FR(466):DE(8), FR(7):BE(2), AT(9):DE(26), FR(4486):BE(48), PL(22):LT(1), AT(281):DE(26), PL(57):DE(47), CH(7):IT(54), FR(182):CH(2), FR(10):CH(183), AT(5):DE(41), FI(33):SE(2), DK(215):DE(10), CH(4):IT(1381), NL(2):DE(458), AT(95):DE(1), PT(1):ES(56), DE(17):CZ(4), DE(106):CZ(83), DE(139):LU(407), PL(11):LT(5), NL(11):DE(16), FI(6):SE(97), NL(9):DE(7), CH(31):LI(548), DE(20):LU(779), PL(68):CZ(56), FR(786):BE(595), FI(14):SE(67), CH(138):IT(56), DE(93):CZ(6), FR(15):BE(1), FR(883):DE(98), NL(426):BE(403), FR(2907):CH(54), FR(480):CH(119), AT(768):CH(142), DE(14):BE(10), NL(11):BE(1370), FR(157):BE(283), AT(4):HU(63), PL(5):LT(1), NL(10):BE(30), FR(1353):CH(266), DE(248):LU(7), FR(65):BE(50), UK-NIR(14):IE(13), FR(18):BE(852), PT(1):ES(10), UK-NIR(11):IE(18), AT(149):CZ(730), FR(5):BE(27), FR(18):BE(9), DE(325):CH(23), SK(45):HU(2317), UK-NIR(4):IE(37), NL(861):DE(21), NL(15):BE(223), DE(74):CH(33), FI(28):SE(33), NL(3):BE(280), UK-NIR(15):IE(22), FR(22):DE(170), CH(31):IT(435), NL(39):BE(14), AT(13):SI(8), AT(145):DE(88), SE(6):NO(5), NL(6):DE(2), NL(75):DE(1798), DE(578):CH(77), DE(90):CZ(1), NL(3):BE(19), FR(28):CH(4436), DE(210):CZ(340), FR(186):BE(12), NL(4):DE(39), SE(14):NO(1), FR(113):CH(98), FR(19):BE(29), DE(169):CH(2), FR(7):BE(92), FI(117):SE(10), FR(30):DE(207), NL(11):DE(11), FI(14):SE(6), NL(407):BE(187), NL(70):DE(61), NL(8):DE(43), DE(49):CZ(97), FR(1175):BE(26), NL(9):BE(9), FR(52):BE(8), DE(375):CZ(14), AT(983):DE(77), FR(9):CH(6), FI(25):SE(7), AT(207):DE(5), NL(1146):DE(7), UK-NIR(3):IE(62), PL(13):CZ(185), FR(45):CH(14), NL(54):DE(18), FR(56):BE(15), PT(42):ES(119), SE(6):NO(2), FR(1881):DE(139), AT(12):HU(152), FR(1508):BE(817), AT(27):DE(10), AT(33):DE(55), AT(2):SI(2), PL(40):CZ(231), AT(24):HU(1), FR(137):BE(33), DE(17):CZ(5), DE(689):CZ(1), DE(4):LU(1153), NL(18):DE(82), DE(281):LU(56), NL(14):DE(411), NL(20):DE(2), AT(3):DE(32), DE(2):CZ(45), NL(3):DE(29), AT(79):DE(5), UK-NIR(6):IE(43), AT(790):CH(2), DE(60):CH(15), PL(22):CZ(14), FR(61):BE(315), SE(7):NO(2), FR(19):BE(1418), DE(43):CH(1305), PL(11):CZ(35), DE(327):LU(167), DE(1):CH(49), NL(3):BE(277), FR(25):BE(2), FR(1):BE(83), PL(9):CZ(175), NL(9):DE(1), NL(79):BE(18), PL(42):CZ(7), CH(125):IT(41), FR(1):BE(1), DE(4):BE(837), AT(145):DE(1), FR(238):BE(7), FR(143):CH(86), FR(19):DE(2), NL(679):DE(5), NL(20):DE(68), AT(110):DE(5), UK-NIR(32):IE(1), NL(11):BE(159), FR(818):ES(760), DK(40):DE(8), NL(5):DE(414), NL(59):DE(22), AT(843):CH(175), NL(1716):DE(24), UK-NIR(48):IE(31), LV(19):LT(14), AT(536):CH(903), AT(174):CH(1977), FR(60):CH(76), FR(77):CH(92), DE(108):BE(3), PL(50):CZ(11), CH(15):IT(94), DE(2914):CH(695), NL(31):DE(200), UK-NIR(27):IE(21), DE(1):CZ(4), DE(42):CZ(76), AT(569):CH(192), PL(13):LT(14), FR(49):ES(187), FR(7):BE(192), FR(172):ES(1314), DK(817):DE(44), DE(150):CZ(11), NL(27):DE(35), UK-NIR(3):IE(22), NL(924):BE(167), FR(93):BE(1), AT(10):CH(60), DE(159):CZ(626), FR(49):CH(185), FR(654):DE(299), SE(7):NO(4), NL(21):DE(10), DE(23):CH(294), DE(736):CH(926), FR(1287):BE(1506), NL(5):DE(95), DE(13):CH(10), PT(2):ES(158), NL(64):BE(6), DE(152):BE(5), FI(7):SE(2), FR(3):CH(39), PL(7):CZ(6), FR(1):BE(41), SI(1304):IT(139), PL(93):CZ(62), FR(2):CH(89), SK(67):CZ(4), PT(9):ES(46), FR(620):DE(4), FI(12):SE(63), FI(17):SE(3), FR(1212):DE(39), SE(4):NO(1), PL(44):SK(4), UK-NIR(3):IE(28), NL(2):BE(6), FR(114):BE(1), NL(5):BE(22), NL(64):DE(2), FR(6):CH(177), DE(115):LU(41), UK-NIR(3):IE(1), AT(2055):DE(32), DE(394):CZ(96), PL(1222):DE(372), NL(12):DE(20), AT(22):DE(1), UK-NIR(10):IE(3), NL(13):DE(50), FR(21):CH(3045), NL(202):DE(324), DE(37):LU(289), FR(12):CH(109), DE(18):LU(454), AT(9):HU(3), PL(14):CZ(167), UK-EAW(2):UK-SCT(15), FR(28):BE(6), NL(6):DE(45), PT(273):ES(317), UK-NIR(83):IE(20), PT(425):ES(157), LU(1):BE(1), NL(36):DE(2), NL(5):DE(1683), DE(1):LU(1131), FR(63):DE(143), NL(2):BE(4), AT(9):HU(2), FR(7):CH(46), NL(42):DE(1), NL(14):DE(4), FR(42):BE(16), FR(12):BE(6), FI(17):SE(2), UK-NIR(16):IE(5), DE(218):CZ(13), UK-NIR(3):IE(27), UK-NIR(82):IE(6), PL(4):DE(538), AT(9):CZ(1026), UK-NIR(3):IE(4), LU(59):BE(1), PL(19):SK(108), FR(1159):CH(63), NL(20):DE(23), FR(104):DE(101), FI(7):SE(7), DE(893):CH(230), UK-NIR(7):IE(37), FR(261):CH(14), FR(206):CH(789), NL(2):BE(2), CH(520):IT(7), DE(212):CH(115), LV(8):LT(53), PL(27):DE(158), AT(1):SI(10), FR(8):BE(135), PL(6):DE(869), SI(108):IT(452), FR(116):BE(12), CH(25):IT(1), FR(718):BE(56), UK-NIR(61):IE(12), PL(39):CZ(4), UK-NIR(48):IE(348), FR(578):DE(221), FR(95):BE(2703), NL(105):DE(1390), FR(46):BE(360), FR(10):BE(21), FR(8):DE(15), EE(37):LV(5), NL(1343):BE(1565), AT(6):SI(20), NL(25):DE(55), FR(94):BE(4285), FR(60):DE(25), NL(13):BE(25), AT(19):DE(18), DE(105):CH(119), DE(51):CZ(418), FR(1308):DE(9), SI(771):IT(10), PL(92):CZ(105), FR(2):CH(109), FR(16):BE(26), NL(2):DE(9), PL(723):CZ(160), SE(10):NO(7), FR(49):BE(151), UK-NIR(48):IE(6), SI(24):IT(205), AT(3):HU(1), DE(23):CH(387), FR(3):BE(162), AT(3):CZ(1), PL(6907):DE(91), PL(61):CZ(4), AT(342):SI(146), PL(39):CZ(2), NL(493):DE(654), AT(22):SI(8), FR(16):BE(25), FR(338):IT(4), AT(32):DE(4), CH(2):IT(641), NL(273):DE(2), PT(54):ES(54), NL(26):DE(3), NL(28):DE(79), NL(5):BE(6), NL(2):DE(5), FI(4):SE(3), NL(49):BE(637), DE(92):CZ(13), FR(122):CH(747), FR(7):CH(1), NL(25):DE(10), FI(19):NO(9), FR(72):DE(497), FR(253):CH(6067), AT(6):SI(8), NL(13):BE(9), NL(51):DE(7), AT(577):DE(83), AT(5):DE(11), FI(8):NO(24), DE(74):LU(41), FR(45):DE(13), NL(3):BE(49), AT(336):DE(22), AT(1):DE(85), UK-NIR(5):IE(10), DE(660):LU(85), NL(11):DE(22), LV(30):LT(16), NL(4):DE(25), FR(10):DE(10), DE(24):LU(30), PL(134):CZ(213), AT(333):SK(148), FR(950):BE(63), AT(5):DE(58), DE(111):CZ(308), FR(7):DE(240), DE(61):CZ(1), DE(1044):BE(1), UK-NIR(57):IE(21), LU(3):BE(152), FR(82):BE(162), FI(10):SE(5), UK-NIR(5):IE(11), UK-NIR(36):IE(17), UK-NIR(7):IE(8), PT(19):ES(127), AT(6):SI(6), NL(20):DE(114), UK-NIR(15):IE(18), SK(428):HU(72), FR(7):BE(41), PL(78):CZ(197), FR(489):CH(40), NL(1107):BE(81), NL(19):BE(11), DE(1):CZ(22), PL(8):LT(87), PT(30):ES(2), PL(715):CZ(85), SK(851):HU(2), FR(157):CH(653), FI(4):SE(7), FR(5):LU(695), NL(3262):DE(1919), FR(106):CH(9), AT(69):DE(722), FR(87):CH(30), UK-NIR(7):IE(5), FR(262):BE(3), PL(54):CZ(76), FR(158):ES(91), PL(30):DE(373), NL(95):BE(3), NL(36):DE(33), DE(118):CZ(116), AT(7):SI(204), SI(99):IT(425), NL(5):BE(9), DE(9):BE(3), NL(15):DE(34), NL(39):DE(13), NL(10):BE(1), FI(19):NO(1), NL(73):BE(2), NL(94):DE(11), FR(27):BE(614), FR(109):BE(111), DE(203):LU(930), UK-NIR(7):IE(114), AT(3):SI(3), DE(24):BE(38), UK-NIR(22):IE(6), FR(10):CH(22), PL(140):CZ(70), PL(84):CZ(76), FI(19):NO(2), NL(35):BE(10), FR(13):CH(48), NL(18):BE(62), NL(828):DE(195), PL(77):CZ(17), FI(1):SE(25), DE(7):CH(1353), AT(3):SI(4), FR(54):IT(39), UK-NIR, FR(19):BE(2), DE(114):CH(52), DE(112):LU(387), FR(4):BE(4), NL(4):BE(746), PL(121):CZ(55), FR(8):BE(35), FR(9):BE(66), NL(67):BE(3), DE(96):LU(3), AT(7):DE(2), AT(21):DE(36), NL(397):DE(7), DE(21):CZ(34), NL(3):DE(33), FI(1):SE(4), PL(2):LT(8), FR(21):ES(5103), NL(25):BE(71), UK-NIR(7):IE(1), FR(7):BE(373), NL(1280):BE(53), DE(266):LU(9), NL(10):BE(7), BG(1521):RO(14), FR(5):BE(12), PL(98):SK(15), NL(191):DE(664), AT(519):CH(776), PL(153):CZ(33), NL(25):BE(74), FR(150):BE(14), DE(798):CZ(613), AT(367):HU(1), DE(74):CZ(158), DE(28):CZ(16), FR(4):LU(2), SE(12):NO(10), PL(15):CZ(37), PL(1):CZ(16), AT(4):SI(34), FI(1):SE(2), NL(5):DE(2), UK-NIR(5):IE(16), FR(12):DE(401), FR(506):DE(186), PL(2945):CZ(2761), PL(18):CZ(94), FI(2):SE(12), DE(954):CZ(26), CH(382):IT(0), LV(17):LT(1), AT(304):DE(546), FR(4):BE(8), DE(8):LU(29), FR(21):DE(130), DK(2):DE(6), DE(24):BE(30), UK-NIR(29):IE(2), FR(47):BE(3), FR(3):BE(76), FR(18):BE(43), DE(20):CH(9), PL(19):CZ(46), SE(6):NO(10), DE(18):CZ(1), UK-NIR(15):IE(12), FI(3):NO(5), FR(47):BE(51), LV(8):LT(274), PL(9):LT(5), AT(246):HU(4), CH(433):IT(345), FR(12):CH(715), UK-NIR(57):IE(1), PL(1):LT(15), FR(47):LU(336), EE(737):LV(9), FR(895):BE(26), FI(3):NO(2), PL(9):LT(2), PL(8):CZ(239), PL(70):CZ(39), DE(2):LU(1869), EE(646):LV(352), DE(4):CZ(39), FR(5661):BE(59), FR(11):BE(60), FR(1470):CH(15), LV(4):LT(5), DE(1):CH(5), FR(547):CH(134), UK-NIR(27):IE(27), NL(311):DE(15), FR(9):BE(21), FI(3):NO(3), PL(2584):CZ(5), FR(5):BE(18), UK-NIR(35):IE(3), AT(132):DE(1), NL(61):BE(199), AT(4):DE(18), PL(138):CZ(6), NL(8):BE(18), DE(2656):CH(13), AT(1):DE(154), NL(43):DE(561), DE(3):LU(22), AT(1115):DE(9), FR(4082):BE(3), DE(231):LU(101), PL(31):DE(149), FR(121):DE(4), AT(43):DE(752), DE(1054):BE(21), FR(322):BE(8), DE(140):BE(21), FR(5):BE(3), AN, DE(572):CZ(2), FR(3):BE(16), EE(1):LV(26), AT, AT(11):DE(24), DE(395):CZ(98), CH(64):LI(60), UK-NIR(3):IE(33), RO(216):HU(97), AT(11):DE(104), DE(757):BE(39), FI(34):SE(17), DE(43):LU(3), BE, BG, PT(1):ES(244), AT(33):CZ(8), FR(7):BE(605), PL(172):CZ(53), FR(10):BE(13), AT(424):DE(82), NL(508):BE(5), NL(2):DE(63), AT(95):CH(367), DE(80):CZ(25), FR(113):BE(5), CH, CH(69):IT:CH(807), NL(468):BE(1043), AT(1):SI(44), NL(8):BE(265), FR(2):BE(40), FR(9):CH(767), AT(2):SI(14), CZ, UK-NIR(213):IE(3), FR(6):BE(12), FI(6):SE(4), FR(1150):CH(341), DE, DE(1):CH(35), FR(41):BE(53), DK, PL(181):CZ(116), FR(508):CH(376), UK-EAW(52):UK-SCT(43), SI(44):HU(16), AT(15):SK(243), SI(170):IT(214), FR(6):BE(19), PT(32):ES(35), NL(6):BE(733), UK-NIR(400):IE(4), NL(120):BE(32), UK-NIR(103):IE(138), AT(18):SI(3), EE, FR(5):CH(96), FR(341):DE(148), FR(2):BE(1), CH(12):IT:CH(1335), FR(300):BE(29), EL, DE(210):LU(520), FI(16):SE(6), AT(53):DE(5), FR(32):BE(3), ES, CH(514):IT(100), AT(12):DE(113), FR(1087):LU(7), UK-NIR(52):IE(77), CH(108):IT(14), AT(28):DE(1649), UK-NIR(28):IE(1), DE(112):BE(26), LV(221):LT(5), PL(5):CZ(309), CH(84):LI(38), NL(2):BE(19), NL(4):DE(34):BE(8), FI, DE(3):BE(11), FR(1071):DE(132), FR, UK-NIR(87):IE(40), FR(7):CH(176), DE(370):BE(19), SE(5):NO(11), FR(10):BE(17), AT(6):DE(47), DE(2711):CH(292), FR(3):BE(14), PL(8):CZ(1128), DE(548):CH(9), PL(24):CZ(660), DE(32):BE(1), DE(12):BE(108), AT(17):DE(25), FR(210):BE(29), FR(89):CH(246), FR(45):BE(25), FR(2):BE(44), PL(503):SK(208), FR(162):CH(10), DE(210):CH(139), AT(94):DE(4), DE(174):CZ(11), NL(5092):DE(378), FR(2953):CH(359), PL(37):CZ(81), AT(11):CZ(5), FI(27):SE(4), AT(7):SI(48), PL(557):DE(2), AT(8):CH(572), PL(5):CZ(132), FR(1):CH(8), NL(12):DE(10), HU, FR(153):BE(14), CH(35):IT(6), IE, NL(12):BE(170), NL(13):BE(581), UK-NIR(1283):IE(178), PL(20):CZ(18), FR(121):BE(83), AT(596):CH(8), DE(29):CZ(12), FR(216):DE(33), FR(107):BE(112), FR(68):ES(1), NL(14):BE(7), IS, IT, AT(34):SI(3), SK(2):HU(346), FR(990):DE(1096), CH(102):LI(167), DE(738):LU(456), NL(9):DE(326), FR(3772):CH(56), PL(192):CZ(128), FR(84):DE(8), PT(90):ES(13), SI(1):HU(2), EE(2):LV(5), UK-NIR(7):IE(44), NL(7):BE(81), DE(357):LU(393), FR(43):BE(83), NL(35):BE(67), FR(11):DE(37), FR(6):DE(67), FR(1):BE(31), DE(906):CZ(21), DE(32):CZ(138), PL(17):DE(34), NL(29):BE(22), NL(56):DE(10), LV(80):LT(2), EE(130):LV(1), FI(3):SE(5), AT(18):SI(1), FR(15):BE(88), PL(63):CZ(283), DE(62):LU(2), DK(57):DE(3), FR(1269):ES(277), FI((13):SE(15), FR(30):CH(39), AT(28):DE(40), AT(13):SI(32), NL(19):BE(12), NL(4):DE(54), DE(780):CH(54), LI, CH(56):IT(541), DE(34):LU(12), AT(39):SI(15), LT, LU, DE(411):CH(4), LV, NL(5):DE(20), FR(15):CH(9), DE(49):CH(461), FR(16):CH(62), DE(17):LU(191), AT(1107):LI(239), NL(62):DE(13), NL(7):DE(177), NL(96):BE(4), NL(24):DE(168), FI(8):SE(164), DE(1041):CH(65), NL(55):DE(46), NL(4):BE(40), FR(384):BE(18), FR(276):CH(251), IT(7990):VA(990), FR(2):DE(210), AT(89):SI(2), FR(6):BE(150), EE(83):LV(44), UK-NIR(108):IE(11), MT, AT(19):SI(77), NL(14):BE(3), SK(31):HU(740), LU(186):BE(347), DE(48):CZ(15), PL(175):SK(17), FR(2):BE(9), NL, NO, AT(1662):DE(121), FR(2):BE(6), DE(649):CH(254), FR(7):CH(2), PL(80):DE(124), DE(20):CZ(4), CH(315):IT(3129), EE(190):LV(414), CH(26):LI(112), CH(39):LI(585), PL(29):CZ(12), CH(19):IT(0), AT(64):DE(9), AT(14):SI(11), AT(1):DE(112), UK-NIR(59):IE(45), FR(2):CH(666), FR(223):LU(183), NL(11):BE(73), FR(5):CH(90), DE(5):CZ(6), DE(27):CH(953), PL(12):DE(3), NL(26):DE(1), LU(2):BE(54), NL(88):DE(230), PL, FR(8):BE(1586), DE(146):CH(980), FR(300):CH(248), FI(3):SE(2), FR(45):DE(23), AT(4):DE(2), FR(18):BE(36), PT, FR(29):CH(117), AT(19):DE(133), CH(7):IT(77), DE(23):CZ(6), FR(393):DE(16), SE(1):NO(1), CH(5):IT(1793), SI(2):IT(38), BG(12):RO(5), NL(10):BE(163), DE(2):CH(46), PL(390):CZ(4), FR(149):DE(58), AT(5):CH(1868), NL(25):DE(22), FR(3):CH(575), AT(393):DE(2), FR(675):ES(10), NL(2):BE(294), SE(1):NO(2), PT(156):ES(109), PL(102):CZ(6), FR(19):CH(4), NL(103):DE(2), FR(39):BE(34), RO, PL(93):CZ(6), NL(55):DE(7), DE(20):CZ(10), NL(1):BE(3), NL(22):DE(13), PL(33):DE(49), DE(389):CH(122), FR(7):LU(1115), NL(35):BE(28), SE, NL(585):BE(13), UK-NIR(14):IE(8), SI, SK, SI(1645):IT(11), PT(26):ES(48), AT(33):DE(312), DE(4):CZ(23), UK-NIR(8):IE(25), DE(65):CZ(1), FR(129):DE(272), AT(16):DE(11), FR(16):BE(116), DE(263):CZ(84), PT(10):ES(101), NL(32):DE(68), FR(2926):CH(539), CH(148):IT(43), LU(221):BE(27), FR(3):CH(19), DE(120):CZ(5), SI(35):IT(2182), AT(12):DE(73), AT(60):SI(4), NL(21):BE(36), UK-NIR(14):IE(6), PL(774):DE(2427), AT(39):DE(63), DE(391):CZ(42), FR(260):BE(30), DE(1061):CH(834), PL(170):CZ(3), PL(30):CZ(2), AT(359):CH(182), UK-NIR(145):IE(16), AT(118):DE(66), NL(2):DE(17), DE(814):CH(202), SE(1):NO(6), NL(11):DE(3), NL(17):DE(45), NL(6):DE(262), FI(2):NO(3), FR(2170):CH(769), FR(2):CH(526), PL(30):CZ(1), DE(23):BE(244), UK-EAW, FR(3):AN(1186), PL(16):DE(4), DE(998):LU(58), NL(43):DE(14), UK-NIR(77):IE(101), NL(7):DE(10), AT(20):DE(4), FR(97):BE(18), AT(12):DE(71), FR(3267):BE(38), DE(1338):CZ(325), DE(118):CZ(127), AT(3):DE(138), AT(64):CZ(15), LU(308):BE(5), NL(1):DE(7), AT(138):DE(6), FR(101):BE(327), FR(9):BE(178), CH(14):IT(105), NL(25):BE(9), FR(6):BE(60), SE(4):NO(6), FR(67):CH(3), NL(3):BE(34), NL(1430):BE(216), NL(22):DE(1), NL(4):DE(58), NL(7):DE(11), DE(559):CZ(3), NL(4):BE(95), PL(36):SK(69), UK-NIR(26):IE(24), FI(19):SE(3), AT(76):DE(14), NL(33):BE(4), DE(45):CZ(3), PT(3):ES(13), FI(20):SE(5), PL(29):CZ(5), AT(11):DE(19), FI(131):SE(108), FR(2068):BE(2050), AT(15):SI(7), DE(306):CH(135), UK-NIR(4):IE(9), FR(2785):CH(4134), AT(1):DE(2), FR(23):BE(135), AT(18):DE(1689), DE(944):CH(193), FR(38):BE(190), UK-NIR(4):IE(11), FR(71):CH(393), LU(42):BE(160), SI(3):IT(9), SK(63):HU(32), FI(2):NO(7), FR(260):CH(71), UK-NIR(62):IE(3), FR(114):ES(49), SK(340):HU(15), DE(547):BE(27), FR(587):DE(143), NL(282):BE(75), NL(72):BE(644), FR(4):CH(2), DE(150):CZ(1), NL(4):BE(8), FR(135):CH(165), NL(15):DE(43), UK-NIR(16):IE(16), NL(2):DE(59), UK-NIR(6):IE(5), NL(18):DE(4), UK-NIR(4):IE(13), FR(6):DE(158), CH(1957):IT(0), AT(14):DE(160), FI(2):NO(5), UK-NIR(25):IE(1), NL(72):BE(16), FR(1163):BE(222), NL(58):BE(248), SE(51):NO(4), DE(7):BE(26), PL(66):CZ(210), NL(133):BE(31), DE(9):CH(24), FR(295):CH(4), CH(53):LI(581), PL(70):DE(218), PT(2):ES(43), NL(52):BE(46), AT(454):DE(60), PL(3466):DE(1161), AT(28):CH(11), SK(1412):HU(162), PT(21):ES(69), FR(121):LU(115), DE(23):CZ(1), FR(14):BE(27), PL(33):SK(26), AT(50):CZ(443), FR(5):BE(7), NL(15):BE(11), FR(4):CH(3), AT(65):DE(39), FI(152):SE(88), DE(337):CH(1362), FR(65):DE(3), FR(10):DE(4), FR(3):BE(313), DE(673):CZ(315), NL(7):BE(45), EE(2513):LV(1193), FI(20):SE(7), LU(18):BE(642), FR(37):BE(35), FR(25):BE(502), NL(69):BE(1991), AT(23):DE(8), AT(17):SI(47), DE(3):CH(10), PT(56):ES(3), UK-EAW(10):UK-SCT(432), FR(778):CH(11), DE(40):CZ(1), NL(171):BE(11), NL(11):BE(2), NL(2):BE(24), NL(8):BE(14), AT(10):DE(48), FR(101):BE(68), DE(21):CH(213), AT(7):SI(4), NL(2208):BE(126), UK-NIR(1):IE(8), PL(181):DE(729), NL(15):BE(8), PT(12):ES(93), FR(125):BE(119), FR(76):CH(377), AT(11):DE(35), AT(19):CZ(29), AT(71):DE(5), PT(215):ES(622), FR(2):BE(944), FI(2):SE(3), UK-NIR(1):IE(9), FR(85):BE(26), PL(139):CZ(68), NL(44):DE(120), FR(6):ES(56), PL(11):SK(5), PL(38):CZ(42), PT(32):ES(29), NL(6):DE(28), LU(164):BE(4), FR(4):BE(217), NL(28):DE(4), DE(6):LU(260), FR(1348):BE(83), CH(25):LI(79), NL(12):BE(18), AT(13):IT(2), FR(51):CH(17), NL(6):BE(1960), DE(10):CH(15), DE(5408):CH(1073), DE(10):CZ(2), FR(5):BE(31), DE(48):CZ(5), NL(29):BE(123), FR(101):BE(375), NL(68):DE(105), NL(1):BE(33), FR(52):ES(1248), NL(20):BE(131), CH(707):IT(1070), PT(25):ES(2), PT(2):ES(177), CH(54):LI(69), PL(62):CZ(154), NL(69):DE(1272), FR(6):BE(25), FI(2):SE(5), NL(100):BE(2), DE(133):CH(2486), SK(4):HU(221), DE(301):CH(177), LV(2):LT(5), UK-NIR(65):IE(8), AT(1148):DE(1660), FR(2431):BE(227), AT(2):DE(1265), SI(596):IT(44), NL(4):DE(8), FI(2):SE(4), FR(237):CH(112), UK-EAW(7):UK-SCT(13), NL(34):BE(8), DE(1039):CH(672), UK-NIR(1):IE(4), PL(28):CZ(16), NL(34):DE(9), AT(43):DE(43), AT(5):SI(5), AT(137):DE(398), FR(647):DE(163), AT(26):CH(1), NL(16):DE(39), NL(106):DE(10), DE(13):CZ(8), SK(19):CZ(15), NL(34):BE(7), DE(33):CZ(402), PL(69):CZ(9), UK-NIR(19):IE(48), SE(2):NO(4), SI(20):IT(168), AT(5):SI(6), EE(1):LV(10), AT(8):SI(4), NL(7):BE(9), NL(9):DE(209), NL(4):DE(4), FI(2):SE(1), AT(17):DE(14), AT(1113):CZ(82), PT(95):ES(12), FR(5509):BE(922), SI(1204):IT(372), FR(35):CH(256), DK(4):DE(4), UK-NIR(169):IE(232), PL(7):LT(6), DE(9):CZ(5), NL(35):DE(219), UK-NIR(1):IE(38), SE(70):NO(3), PL(1112):DE(804), FR(7):BE(528), NL(24):DE(58), PL(8):CZ(255), SK(3):CZ(2), NL(2091):DE(54), PL(75):CZ(9), NL(4):DE(3), PL(53):CZ(204), NL(362):BE(134), UK-NIR(125):IE(21), FR(43):CH(10), AT(1):SI(76), DE(31):LU(27), FR(128):BE(1), CH(66):LI(3), AT(23):DE(203), AT(655):CZ(8), SE(8):NO(24), FR(33):BE(88), DK(2):DE(51), AT(14):DE(8), NL(3):BE(67), AT(586):SI(612), FR(11):BE(1), CH(64):IT(617), NL(25):DE(3), DE(9):CZ(7), NL(1277):BE(8), NL(19):DE(16), AT(5):SI(43), DK(1):DE(2), FR(79):BE(806), NL(7):DE(7), FR(10):CH(6), FR(17):BE(1), UK-NIR(9):IE(1), CH(98):IT(52), UK-NIR(17):IE(12), DE(6):CH(10), FR(14):DE(3), DE(86):CH(604), AT(433):LI(95), UK-NIR(7):IE(56), NL(18):BE(6), DE(799):CH(54), UK-NIR(10):IE(40), EE(6):LV(46), AT(7):SI(34), NL(4):DE(89), SI(567):IT(211), LV(19):LT(75), FR(241):CH(1170), FR(2531):CH(157), DE(10):LU(12), DE(142):CH(154), UK-NIR(21):IE(2), AT(33):DE(3), NL(18):BE(5), PL(107):DE(24), PL(174):SK(6), FR(35):CH(13), NL(12):BE(11), NL(19):BE(335), NL(7):DE(16), FR(84):BE(38), NL(7):BE(595), UK-NIR(23):IE(35), NL(10):DE(19), DE(1546):CH(480), CH(1):LI(244), AT(112):DE(4), FR(30):DE(2), FR(10):CH(9), NL(100):DE(5), FR(686):DE(170), SI(22):HU(2), FI(2):SE(7), PT(4):ES(2), CH(1267):IT(7), DE(10):CZ(1), DE(178):CZ(7), NL(40):BE(9), FR(1):BE(68), DE(42):BE(1), FR(2343):BE(176), FR(454):CH(3095), UK-NIR(1):IE(33), UK-NIR(2):IE(11), FR(22):BE(12), NL(7):BE(1), PL(12):DE(195), UK-NIR(92):IE(6), FR(3):BE(2), PL(12):CZ(1), NL(13):DE(34), FR(45):BE(311), PT(28):ES(77), FR(602):DE(970), FR(369):BE(2), FR(2):CH(2), PL(19):DE(347), UK-NIR(27):IE(2), FR(155):CH(38), UK-NIR(1):IE(30), UK-NIR(2):IE(12), FR(18):CH(463), FR(10):CH(8), UK-NIR(65):IE(4), DE(11):CZ(78), NL(9):BE(202), AT(6):SI(45), UK-NIR(59):IE(6), FR(22):BE(10), NL(160):DE(9), SK(40):CZ(18), PL(13):CZ(116), NL(20):BE(17), CH(7):LI(15), UK-NIR(11):IE(20), UK-NIR(33):IE(27), PL(372):CZ(2222), NL(2):BE(77), FR(2):CH(13), PL(10):CZ(18), CH(551):IT(103), NL(2894):DE(747), FR(250):CH(7), DE(271):CH(635), UK-EAW41):UK-SCT(9), UK-NIR(15):IE(2), PL(26):CZ(32), NL(97):DE(1), AT(343):CH(542), UK-NIR(8):IE(11), NL(98):DE(216), AT(34):DE(12), FR(156):BE(3), AT((5):SI(19), NL(3):BE(28), AT(992):DE(87), UK-NIR(52):IE(2), DE(32):CZ(252), PL(278):CZ(102), NL(40):BE(319), SE(6):NO(30), NL(15):DE(868), FI(11):SE(18), LU(1):BE(14), PL(2):CZ(28), FR(639):DE(1518), AT(18):DE(2), DE(126):CZ(23), LU(15):BE(32), FR(1284):DE(11), FR(2):CH(11), FR(973):DE(668), FR(759):BE(92), FR(8):BE(17), AT(232):DE(16), FR(33):LU(32), AT(20):DE(24), FI(8):SE(22), CH(73):IT(172), SI(160):IT(113), AT(23):DE(30), UK-NIR(39):IE(11), UK-NIR(24):IE(73), FR(1297):DE(63), FR(13):LU(124), PL(5):CZ(698), NL(2018):BE(471), FR(2804):DE(152):CH(73), NL(6):BE(299), UK-NIR(1):IE(75), NL(16):BE(11), PT(36):ES(88), FR(173):BE(25), NL(7):DE(2), FR(3364):BE(91), NL(8):DE(15), DE(1397):BE(49), FI(1):NO(4), PL(5):CZ(124), PL(112):SK(110), AT(17):CZ(2), LU(1):BE(11), FR(33):CH(1111), PL(1618):CZ(218), NL(1241):BE(112), FR(134):BE(1), PL(6):CZ(248), AT(69):SK(37), AT(42):DE(12), PL(106):CZ(12), NL(23):BE(20), NL(15):DE(14), FR(19):DE(2998), AT(55):CH(28), DE(76):BE(96), DE(273):CZ(3), PL(6):SK(229), NL(28):BE(8), PL(283):SK(3), NL(79):DE(15), FR(579):CH(5), FR(4):BE(88), FR(113):CH(68), AT(1678):DE(171), DE(767):BE(73), FR(137):BE(147), CH(124):IT(705), UK-NIR(11):IE(27), AT(8):DE(43), NL(8):BE(66), CH(51):IT(0), FR(694):DE(41), AT(146):DE(7), AT(1152):DE(264), FR(113):CH(20), SK(32):HU(533), AT(9):SI(16), NL(2):DE(26), AT(2034):CH(15), PL(123):DE(166), NL(1562):DE(13), DE(8):CZ(589), CH(53):LI(89), FR(219):BE(31), FR(39):BE(29), NL(17):DE(317), PL(29):CZ(526), NL(12):DE(1), AT(12):DE(117), PL(4):SK(41), AT(21):DE(155), DE(820):CH(800), FR(20):BE(2), FR(520):BE(2), AT(1):HU(3), SK(302):HU(396), DE(27):CZ(44), FR(2):CH(518), FR(1157):CH(24), SI(1):IT(35), AT(26):DE(251), NL(937):BE(11), UK-NIR(5):IE(2), PL(17):LT(7), PL(297):CZ(603), DE(1475):CH(3), UK-NIR(34):IE(15), AT(15):CH(86), DE(40):CH(18), NL(862):DE(1), AT(64):DE(14), AT(3):SI(65), FR(9):BE(40), NL(28):DE(13), NL(1628):DE(86), NL(23):DE(16), FR(5):BE(39), FR(109):BE(2), AT(47):CH(249), FR(36):BE(1), AT(1857):CH(334), NL(88):DE(11), FR(247):BE(1906), UK-NIR(163):IE(198), NL(8):BE(22), FR(4):BE(352), NL(16):DE(1124), SK(91):CZ(11), DE(6):CH(2), SE(5):NO(5), DE(177):LU(94), AT(52):DE(4), UK-NIR(18):IE(7), CH(14):IT(287), AT(5):DE(200), DE(48):BE(9), AT(51):CZ(1), PT(180):ES(7), NL(16):BE(12), AT(5):DE(6), PL(25):CZ(1), NL(11):DE(664), DE(3):CZ(4), FR(26):CH(24), UK-NIR(5):IE(5), FI(8):NO(3), FR(9):DE(671), FR(663):DE(266), UK-NIR(30):IE(3), FI(223):SE(22), SK(2):HU(528), LU(164):BE(449), DE(512):CH(57), NL(2):DE(20), FR(194):CH(194), AT(2):DE(335), AT(20):SI(2), DE(3):CZ(1), FR(142):CH(2), DE(308):CZ(39), NL(12):DE(2), NL(3):BE(29), PL(356):CZ(623), SI(140):IT(284), PL(3871):DE(466), FR(3):BE(107), DE(1):CH(60), LV(29):LT(10), CH(75):IT(234), FR(70):BE(1), FR(36):BE(5), FR(81):DE(59), DE(8):CZ(20), AT(1652):CH(40), NL(36):DE(197), PL(27):SK(18), CH(114):IT(4), NL(3):DE(12), FR(18):ES(86)]
			//Dimension: METHD_CL (3 dimension values)
			//  [A, D, M]

			logger.info("Format...");
			for(Stat s : popData.stats)
				s.dims.put("GRD_ID", reformatGeostatGridId(s.dims.get("GRD_ID")));

			logger.info("Save...");
			CSV.save(popData, "TOT_P", GridsProduction.basePath+"pop_grid/pop_grid_2006_1km_full.csv");
		}*/


		/*logger.info("2011");
		{
			//NB: for 2011, population figure are stored in two deparate files

			logger.info("Load data 1...");
			StatsHypercube popData = CSV.load(GridsProduction.basePath+"pop_grid_1km_geostat_raw/2011.csv", "TOT_P");

			//popData.printInfo(false);
			//Dimension: GRD_ID (2024787 dimension values)
			//Dimension: DATA_SRC (31 dimension values)
			//  [NO, DE, FI, BE, PT, BG, DK, LT, LV, HR, FR, HU, SE, UK, SI, XK*, SK, IE, EE, CH, EL, MT, IT, AL, ES, AT, CZ, PL, RO, LI, NL]
			//Dimension: YEAR (2 dimension values)
			//  [2011, 2010]
			//Dimension: CNTR_CODE (31 dimension values)
			//  [NO, DE, FI, BE, PT, BG, DK, LT, LV, HR, FR, HU, SE, UK, SI, XK*, SK, IE, EE, CH, EL, MT, IT, AL, ES, AT, CZ, PL, RO, LI, NL]
			//Dimension: METHD_CL (3 dimension values)
			//  [A, D, M]
			//Dimension: TOT_P_CON_DT (6 dimension values)
			//  [0, other, 3, 4, 5, 10]

			logger.info("Load data 2...");
			StatsHypercube popDataJrc = CSV.load(GridsProduction.basePath+"pop_grid_1km_geostat_raw/2011_jrc.csv", "TOT_P");

			//popDataJrc.printInfo(false);
			//Dimension: GRD_ID (81684 dimension values)
			//Dimension: DATA_SRC (2 dimension values)
			// [ESTAT/AIT, JRC]
			//Dimension: YEAR (1 dimension values)
			// [2011]
			//Dimension: CNTR_CODE (12 dimension values)
			// [RS, AD, IM, CY, MC, ME, LU, SM, IS, VA, MK, BA]
			//Dimension: METHD_CL (1 dimension values)
			// [D]
			//Dimension: TOT_P_CON_DT (1 dimension values)
			// [0]

			logger.info("Merge data...");
			StatsIndex si = new StatsIndex(popData, "GRD_ID");
			for(Stat s : popDataJrc.stats) {
				Stat s_ = si.getSingleStat( s.dims.get("GRD_ID") );
				if(s_ == null) popData.stats.add(s);
				else s_.value += s.value;
			}
			popDataJrc = null;


			logger.info("Format...");
			for(Stat s : popData.stats)
				s.dims.put("GRD_ID", reformatGeostatGridId(s.dims.get("GRD_ID")));

			logger.info("Save...");
			CSV.save(popData, "TOT_P", GridsProduction.basePath+"pop_grid/pop_grid_2011_1km_full.csv");
		}*/

		logger.info("2018");
		{
			logger.info("Load data...");
			StatsHypercube popData = CSV.load(GridsProduction.basePath+"pop_grid_1km_geostat_raw/2018.csv", "TOT_P_2018");

			//popData.printInfo(false);
			//   Dimension: OBJECTID (2416631 dimension values)
			//   Dimension: CNTR_ID (147 dimension values)
			//   Dimension: TOT_P_2018 (12608 dimension values)
			//   Dimension: TOT_P_2006 (11722 dimension values)
			//   Dimension: Shape_Area (1 dimension values)
			//   Dimension: Country (39 dimension values)
			//   Dimension: GRD_ID (2416631 dimension values)
			//   Dimension: Method (7 dimension values)
			//   Dimension: Shape_Length (1 dimension values)
			//   Dimension: TOT_P_2011 (12591 dimension values)
			//   Dimension: Date (8 dimension values)

			logger.info("Save...");
			CSV.save(popData, "TOT_P", GridsProduction.basePath+"pop_grid/pop_grid_2018_1km_full.csv");
		}

	}

	//in: 1kmN2405E4337 out: CRS3035RES200mN1453400E1452800
	static String reformatGeostatGridId(String geostatId) {
		String id = geostatId.replace("E", "000E");
		id = id.replace("1kmN", "CRS3035RES1000mN");
		id = id+"000";
		return id;
	}

}
