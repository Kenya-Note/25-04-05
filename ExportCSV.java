import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
// 21/6/26 追加
import java.util.Map;

public class ExportCSV {
	
	// 21/12/29 Exportするデータセットの規定
	private Flag.Export exportFlag = Flag.Export.DifferenceODI;

	// 21/7/1 出力するHFTエージェントタイプ数
	private static int TYPES_OF_HFTs = 1;
	// csvファイル出力の周期
	private static int EXPORT_SPAN = 10000;
	
	// 21/5/8 追加 csvファイル出力の開始期と終了期
	// 2024/09/03 シミュレーション終了まで記録
	private static int EXPORT_START = 30000;
	private static int EXPORT_END = 20000000;
//	private int export_end;

	private String fileName;
	private FileWriter f;
	private PrintWriter p;

	private String fileDate;
	private String folder;

	// アプリケーション個別フォルダ 21/12/23
	private File appDir;
	
	// 21/7/1 HFTタイプ数取得用
	public ExportCSV() {
	}

	// 21/5/5 変更
//	public ExportCSV(Data data, int appNumber, String date){
	public ExportCSV(Data data, int appNumber, Flag.HFT hft_flag, String date){
		try {
			// 21/12/30
			int j = 0 ;
			
			// ファイルセパレータ
        	String separator = File.separator;

        	// 21/12/9 CSVファイル名に付加するエージェント種別
        	String agent_type ="";

        	// フォルダを作成
        	this.folder = "Result_CSV";
			File newDir = new File(folder);
			newDir.mkdir();
			// 試行別フォルダの作成
			this.fileDate = newDir.getAbsolutePath() + separator + date;
			// 21/12/22 アプリ別フォルダの廃止
//			File dateDir = new File(fileDate);
//			dateDir.mkdir();
			appDir = new File(fileDate);
			appDir.mkdir();

			// 21/12/9 変更（agent_typeを追加）
			// appNumberフォルダの作成
			
			agent_type = hft_flag.toString() ;
			
			// 21/12/23 変更（アプリ別フォルダの廃止）
//			String appNum = "app" + appNumber + "_" + agent_type;
//			String appNum = "app" + appNumber;
//			appDir = new File(dateDir.getAbsoluteFile() + separator + appNum);
//			appDir.mkdir();

			// "Result_CSV/YYYYMMDD_HHMM/appn_AGENTTYPE/appn_AGENTTPYE_YYYMMDDHHMM.csv"
			// 21/5/6 バージョン管理用に日付をいれる
//			this.fileName = appDir.getAbsolutePath() + separator + "app" + appNumber +  "_data_" + date + ".csv";
			// 21/12/9 変更（agent_typeを追加）
			this.fileName = appDir.getAbsolutePath() + separator + appNumber +  "_" + agent_type + "_" + date + ".csv";
			this.f = new FileWriter(fileName, false);
        	this.p = new PrintWriter(new BufferedWriter(f));

        	// 属性を指定
        	// 21/12/29
        	if (exportFlag == Flag.Export.DATAFULLSET) {
        		// 22/2/9 フィールドからの書き出し変更
            	// for(Field field : data.getClass().getDeclaredFields()){
            	//  	p.print(field.getName() + ",");
            	// }
            	// p.println();
	            for (j = 0; j < data.getAttributeListforFullData().size()-1; j++) {
	            	p.print(data.getAttributeListforFullData().get(j) + ",");
	            }
            	p.println(data.getAttributeListforFullData().get(j));
        		
			} else if (exportFlag == Flag.Export.DifferenceODI) {
	            for (j = 0; j < data.getAttributeListforDifferenceODI().size()-1; j++) {
	            	p.print(data.getAttributeListforDifferenceODI().get(j) + ",");
	            }
            	p.println(data.getAttributeListforDifferenceODI().get(j));
        	} 
        	// ファイルを閉じる
        	p.close();
        	f.close();
		} catch (IOException ex) {
			System.out.println("cannot make file");
            ex.printStackTrace();
        }
	}
	/**
	 * 21/06/24 星野
	 * 最終結果出力用
	 * @param date 日付フォルダの名前
	 */
	public ExportCSV(String date) {

		String separator = File.separator;

		this.folder = "Result_CSV";

		// 出力用のフォルダがない場合は作成
		File newDir = new File(folder);
		if(!newDir.exists()) {
			newDir.mkdir();
		}

		this.fileDate = newDir.getAbsolutePath() + separator + date;
	}

	/**
	 * 21/6/24 星野
	 * 最終結果出力用
	 * @param results 出力したいデータ名（Key: String）とデータ（Value: Object）の連想配列
	 * @param name 出力先ファイル名
	 */
	public void exportCsv(Map<String, Object> results, String name) {

		String separator = File.separator;

		// name名のファイルを作成
		File dateDir = new File(fileDate);
		if(!dateDir.exists()) {
			dateDir.mkdir();
		}

		this.fileName = dateDir.getAbsolutePath() + separator + name;

		try {
			this.f = new FileWriter(this.fileName, false);
			this.p = new PrintWriter(new BufferedWriter(f));

			// 連想配列のkeyとvalueをカンマ区切りで出力
			for(Map.Entry<String, Object> entry : results.entrySet()) {
				p.println(String.format("%s, %s", entry.getKey(), entry.getValue()));
			}

			p.close();
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 板情報（未使用）
	public String makeCsvFile(String name) {
		String fileName = "";
		try {
			// 出力ファイルの作成
        	String separator = File.separator;
			fileName = folder + separator + name + fileDate + ".csv";
        	FileWriter fi = new FileWriter(fileName, false);
        	PrintWriter pr = new PrintWriter(new BufferedWriter(fi));
        	pr.close();
        	fi.close();
		} catch (IOException ex) {
			System.out.println("cannot make file");
            ex.printStackTrace();
        }
    	return fileName;
	}

//	public void exportArrayCsv(String fileName, double[] array) {
//		// 出力ファイルの作成
//    	try {
//			f = new FileWriter(fileName, true);
//			p = new PrintWriter(new BufferedWriter(f));
//			// ファイルを閉じる
//			p.close();
//        	f.close();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
//	}

	// Dataクラスの結果をCSV形式で出力
	public void exportCsv(int time, Data data, Market market, Parameter param, HFT hft, NormalAgent nagent){
		
		// 21/4/28 
		long i=0 ;
		int j = 0;
		
		// 出力ファイルの作成
    	try {
			this.f = new FileWriter(this.fileName, true);
			this.p = new PrintWriter(new BufferedWriter(f));

			// 内容をセット
        	// 21/12/29
        	if (exportFlag == Flag.Export.DATAFULLSET) {
				// 22/2/9 フィールド書き出しから変更
				//				List<String> line = new ArrayList<String>();
				//				for(Field field : data.getClass().getDeclaredFields()){
				//					try {
				//						field.setAccessible(true);
				//						// 各時系列データのを格納するリスト
				//						List<Object> list = (List<Object>) field.get(data);
				//						
				//						if(list.size() > 0) {
				//							// 各指標の最新データを取得
				//							line.add(String.valueOf(list.get(list.size()-1)));
				//						}
				//						else {
				//							line.add("null");
				//						}
				//					} catch (IllegalAccessException e) {
				//				        System.out.println("IllegalAccessException: " + field.getName() + "_ACCESS_DENIED");
				//						e.printStackTrace();
				//					}
				//					
				//					i++;
				//				}
				//				p.println(String.join(",", line));
        		List<Object> listForFullData = data.getListforFullData(time, market, param, hft, nagent);
	            for (j = 0; j < listForFullData.size()- 1; j++) {
	            	p.print(listForFullData.get(j) + ",");
	            }
	            p.println(listForFullData.get(j));
			} else if (exportFlag == Flag.Export.DifferenceODI) {
        		List<Integer> listForSubsetData = data.getListforDifferenceODI(time, market, hft);
	            for (j = 0; j < listForSubsetData.size()- 1; j++) {
	            	p.print(listForSubsetData.get(j) + ",");
	            }
	            p.println(listForSubsetData.get(j));
			}
			// ファイルを閉じる
			p.close();
        	f.close();
			// System.out.println("ファイル出力完了");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    }

	// Dataクラスの結果を一括でCSV形式で出力
//	public void exportCsv3(Data data){
//		// 出力内容
//		String all_data = "";
//		// 出力ファイルの作成
//    	try {
//			this.f = new FileWriter(this.fileName, true);
//			this.p = new PrintWriter(new BufferedWriter(f));
//
//			// csv出力
//			for(int i=data.getDataLength(); i>=1; i--) {
//				// 内容をセット
//				List<String> line = new ArrayList<String>();
//				for(Field field : data.getClass().getDeclaredFields()){
//					try {
//						field.setAccessible(true);
//						List<Object> list = (List<Object>) field.get(data);
//						//
//						if(list.size() >= data.getDayDataLength()) {
//							line.add(String.valueOf(list.get(list.size()-i)));
//						}
//						else {
//							int j = (int) Math.ceil((double)i / (double)data.getDayDataLength());
//							line.add(String.valueOf(list.get(list.size()-j)));
//						}
//					} catch (IllegalAccessException e) {
//				        System.out.println("IllegalAccessException: " + field.getName() + "_ACCESS_DENIED");
//						e.printStackTrace();
//					}
//				}
//				all_data = all_data + String.join(",", line) + "\n";
////				p.println(String.join(",", line));
//			}
//			p.print(all_data);
//
//			// ファイルを閉じる
//			p.close();
//        	f.close();
//			// System.out.println("ファイル出力完了");
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
//    }

	// getter
	public String getAppDirPath() {
		return appDir.getAbsolutePath();
	}
	
	public int getExportSpan() {
		return EXPORT_SPAN ;
	}

	// 21/5/8 追加 csvファイル出力の開始期と終了期
	public int getExportStart() {
		return EXPORT_START ;
	}
	
	public int getExportEnd(int t_end) {

		if (t_end < EXPORT_END) return (t_end - 1);
		else return EXPORT_END;

	}
	
	// 21/7/1 出力するHFTの種別数
	public int getTypesofHFTs() {
		return TYPES_OF_HFTs ;
	}
	
	// 21/12/29 exportする
	public Flag.Export getExportFlag() {
		return exportFlag ;
	}
}
