import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JOptionPane;

// Application.java line 270
  // // 25/03/22 ひとまず、FundamentalAgentの場合は記録しない

public class Main {
	
	//プログラム試行回数（並列）
	private static int trialNum = 1;

	// リリース時の並列実行数
	private static int runLimit = 2;

	public static void main(String[] args) {

		Flag.Run runFlag = Flag.Run.RELEASE;

		switch (runFlag) {
			case DEBUG:
				debugRun();
				break;
			case RELEASE:
				releaseRun();
				break;
			default:
				break;
		}
	}

	// ファイル名における日時
	public static String fileDate(){
		LocalDateTime d = LocalDateTime.now();
		// 表示形式を指定
		DateTimeFormatter df1 = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
		String s = df1.format(d);
		return s;
	}

	// デバッグ用の実行（従来通り）
	public static void debugRun() {
		ExportCSV csv = new ExportCSV();
		int typeofHFTs = csv.getTypesofHFTs();

		// パラメータクラス
		Parameter param[];
		param = new Parameter[trialNum * typeofHFTs + 1];

		// 市場の価格決定方式
		// true: ザラ場
		// false: 板寄せ
		boolean doubleAuction;
		// 21/4/25 HFTエージェントの種別
		Flag.HFT hft_flag;
		// データ出力の有無
		boolean output;

		// 21/5/5 変更 w1を消去
		// 実行日時（フォルダ名）
//		String date = "w1_" + fileDate();
		String date = fileDate();
		// 実行クラス
		Application app[];
		// 21/7/1 HFTなしを i = trialNum * 2 番目に格納
		app = new Application[trialNum * typeofHFTs + 1];

		// 21/7/1 HFTなしを i = trialNum * 2 番目に格納
		for(int i = 0; i <= trialNum * typeofHFTs; i++) {
			// デフォルトの値に設定
			param[i] = new Parameter(i);
		}

		// シミュレーション実行
		// 21/6/28 HFTなしをHFTの任意の戦略に変更
		for(int i = 0; i < trialNum * typeofHFTs; i++) {
			if (i == trialNum * typeofHFTs - 1) output = true ; // trialNumのみ表示用に利用する
			else output = false ;

			if(i < trialNum) {
				// パタン1
				// 21/12/29 getExportFlag追加
				app[i] = new Application(i, param[i], date, doubleAuction=true, hft_flag = Flag.HFT.PrOMM, output);
				app[i].start();
			}
			else if (i < trialNum * (typeofHFTs - 1)){
				// パタン2（2パタンのみ収集する場合はここがnopになるはず．）
				// 21/12/29 getExportFlag追加
				app[i] = new Application(i, param[i], date, doubleAuction=true, hft_flag = Flag.HFT.PrOMM, output);
				app[i].start();

			}
			else {
				// パタン3
				// 21/12/29 getExportFlag追加
				app[i] = new Application(i, param[i], date, doubleAuction=true, hft_flag = Flag.HFT.PrOMM, output);
				app[i].start();

			}
		}

		// 21/7/1 HFTなしを i = trialNum * 2 番目に格納
		// 21/12/29 getExportFlag追加
		app[trialNum * typeofHFTs] = new Application(trialNum * typeofHFTs, param[trialNum * typeofHFTs], date, doubleAuction=true, hft_flag = Flag.HFT.NONE, false);
		app[trialNum * typeofHFTs].start();

		// シミュレーション開始表示
		// 21/7/1 HFTなしを i = trialNum * 2 番目に格納
		System.out.println("\nSimulation 0 ~" + (trialNum * typeofHFTs) + " : Start");

		// 21/7/1 HFTなしを i = trialNum * 2 番目に格納
		for(int i = 0; i <= trialNum * typeofHFTs; i++) {
			try {
				app[i].join();
			} catch (InterruptedException d) {
				System.out.println(d);
			}
		}

		// 21/12/30 シミュレーション終了期をresult.aggregateに渡す
		int t_end = param[0].getT_end();

		// 21/6/24 最終データ集計  追加 星野
		Result result = new Result(app, date);
		result.aggregate(t_end);

		System.out.println("終わりです");
		JOptionPane.showMessageDialog(null, "終わったよ");

	}

	// 実験計測用の実行
	public static void releaseRun() {
		ExportCSV csv = new ExportCSV();
		int typeofHFTs = csv.getTypesofHFTs();

		// パラメータクラス
		Parameter[] param;
		param = new Parameter[trialNum * typeofHFTs + 1];

		boolean output;

		String date = fileDate();
		Application[] app = new Application[trialNum * typeofHFTs + 1];
		
		// 2024/07/03
		// NormalAgent.classの条件分岐に利用
		for(int i = 0; i <= trialNum * typeofHFTs; i++) {
			param[i] = new Parameter(i);
		}
		
		// 実行クラスの準備
		for(int i = 0; i < trialNum * typeofHFTs; i++) {
			// 先頭分のみ表示
			if(i % runLimit == 0) {
				output = true;
			} else {
				output = false;
			}

			// ここで実行するHFTのタイプを決める
			if(i < trialNum) {
				app[i] = new Application(i, param[i], date, true, Flag.HFT.NONE, output);
			}	else if (i < trialNum * (typeofHFTs - 1)){
				app[i] = new Application(i, param[i], date, true, Flag.HFT.NONE, output);
			} else if (i < trialNum * typeofHFTs){
				app[i] = new Application(i, param[i], date, true, Flag.HFT.NONE, output);
			}
		}
		app[trialNum * typeofHFTs] = new Application(trialNum * typeofHFTs, param[trialNum * typeofHFTs], date, true, Flag.HFT.NONE, false);

		// 実験開始
		for(int i = 1; i <= app.length; i++) {
			app[i - 1].start();
			// runLimit分のみ動かして残りは待機
			try {
				if(i % runLimit == 0) {
					for(int j = 1 + i - runLimit; j <= i; j++) {
						app[j - 1].join();
					}
				}
			}  catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// 最後に全て同期をとる
		for(int i = 1; i <= app.length; i++) {
			try {
				app[i - 1].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		int t_end = param[0].getT_end();

		Result result = new Result(app, date);
		result.aggregate(t_end);

		System.out.println("終わりです");
	}

}
