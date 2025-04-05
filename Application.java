public class Application extends Thread {

	// エージェントクラス（Agent型に変更）
	private Agent[] agentArray;
	
	// 25/03/23 CSV出力用のNormalAgent
	private NormalAgent dummyNormalAgent;
	// マーケットメイカークラス
	private HFT hft;
	// マーケットクラス
	private Market market;
	// 時系列データクラス
	private Data data;
	// CSV出力クラス
	private ExportCSV csv;
	// パラメータクラス
	private Parameter param;
	// エージェントの数
	private int agentNum;
	// FundamentalAgentの数
	private int fundamentalAgentNum = 100; // 100体のFundamentalAgentを配置
	// TechnicalAgentの数
	private int technicalAgentNum = 100; // 100体のTechnicalAgentを配置
	// シミュレーション時刻
	private int time;
	// シミュレーション期間
	private int time_end;
	// 板形成期間
	// 21/12/31 名称変更time_book -> time_bookMake
	private int time_bookMake;
	// 市場の価格決定方式について
	private boolean doubleAuction;
	// 板情報記録用csvファイル名
	private String bookFile;
	// 21/4/26 HFTエージェントの有無
	//private boolean hft_flag;
	private Flag.HFT hft_flag;
	// アプリケーションの固有ナンバー
	private int appNumber;
	// データ出力の有無
	private boolean output;
	// 実行日時
	private String date;
	// アプリケーション個別フォルダ
	private String appDir;
	
	// カウント用の変数
	private int i;
	// 取引するエージェントのインデックス
	private int index;
	

	public Application(int appNumber, Parameter param, String date, boolean doubleAuction, Flag.HFT hft_flag, boolean output) {

		// カウント用の変数
		this.i = param.getI();
		
		// アプリケーションの固有ナンバー
		this.appNumber = appNumber;
		// データ出力の有無
		this.output = output;

		agentNum = param.getAgentNum();
		// エージェントクラス
		agentArray = new Agent[agentNum];

		
		// 各エージェントを初期化（まずすべてNormalAgentで初期化）
		for(int j=0; j<agentArray.length; j++) {
			// 一意のエージェントIDを使用
			agentArray[j] = new NormalAgent(param, j);
		}
		
		// 25/03/23 CSV出力用のダミーNormalAgentを初期化
		this.dummyNormalAgent = new NormalAgent(param, 0);
		
		// ランダムな位置にFundamentalAgentを配置（重複なし、連続配置なし）
		java.util.Random randFA = new java.util.Random();
		java.util.Set<Integer> FApositionSet = new java.util.HashSet<>();
		java.util.Set<Integer> unavailablePositions = new java.util.HashSet<>();
		
		/*
		while (FApositionSet.size() < fundamentalAgentNum) {
			// 配列の最後は許可しない
			int position = randFA.nextInt(agentNum - 1);
			// 新しい位置かつ、連続配置にならない場合のみFundamentalAgentを配置
			if (!unavailablePositions.contains(position) && FApositionSet.add(position)) {
				agentArray[position] = new FundamentalAgent(param, position);
				// FundamentalAgentを配置した位置の前後も使用不可にする（連続配置を防ぐ）
				if (position > 0) { // 配列の最初でない場合
					unavailablePositions.add(position - 1);  // 前の位置
				}
				if (position < agentNum - 1) { // 配列の最後でない場合
					unavailablePositions.add(position + 1);  // 後の位置
				}
			}
		}
		*/

		/*
		// ランダムな位置にTechnicalAgentを配置（重複なし、かつFundamentalAgentとも重複しない）
		java.util.Random randTA = new java.util.Random();
		java.util.Set<Integer> TApositionSet = new java.util.HashSet<>();

		while (TApositionSet.size() < technicalAgentNum) {
			int position = randTA.nextInt(agentNum);
			// すでにFundamentalAgentが配置されていない、かつ新しい位置である場合のみ追加
			if (!FApositionSet.contains(position) && TApositionSet.add(position)) {
				agentArray[position] = new TechnicalAgent(param, position);
			}
		}
		*/
		
		if (output) {
			// 実際にFundamentalAgentが何体配置されたか確認
			int actualFundamentalAgentCount = 0;
			for (Agent agent : agentArray) {
				if (agent instanceof FundamentalAgent) {
					actualFundamentalAgentCount++;
				}
			}
			System.out.println("FundamentalAgent配置数: " + actualFundamentalAgentCount);
			// 実際にTechnicalAgentが何体配置されたか確認
			int actualTechnicalAgentCount = 0;
			for (Agent agent : agentArray) {
				if (agent instanceof TechnicalAgent) {
					actualTechnicalAgentCount++;
				}
			}
			System.out.println("TechnicalAgent配置数: " + actualTechnicalAgentCount);
		}

		// マーケットメイカー
		this.hft = new HFT();
		// 21/4/26 HFTエージェントの有無
		this.hft_flag = hft_flag ;
		// マーケットクラスの初期化
		this.market = new Market(param);
		// データクラスの初期化
		this.data = new Data(param.getT_end() + 1);
		// 21/12/9 変更
		// CSV出力クラス
		this.csv = new ExportCSV(data, appNumber, hft_flag, date);
//		this.csv = new ExportCSV(data, hft_flag, date);
		// 試行別フォルダ
		this.appDir = csv.getAppDirPath();
		// パラメータクラス
		this.param = param;
		// パラメータのテキスト保存
		this.param.exportParam(date, appNumber, hft_flag);
		// パラメータの表示
		this.param.dispParam(appNumber, hft_flag);

		// 板情報記録用csvファイルを作成
//		this.bookFile = this.csv.makeCsvFile("bookData");

		// シミュレーション時刻
		time = 0 ;
		
		// シミュレーション期間
		time_end = param.getT_end();
		// 21/12/31 名称変更time_book -> time_bookMake
		time_bookMake = param.getT_bookMake();
		// 市場の価格決定方式について
		this.doubleAuction = doubleAuction;
	}

	public void run() {
		
		// 21/12/31
		while (time < this.time_end) {
			// 20250325 取引するエージェントのインデックス
			this.index = time % this.agentNum; 

			// ザラ場（連続ダブルオークション）方式
			// 21/12/31 名称変更time_book -> time_bookMake
			if(this.doubleAuction) {
				if (time<this.time_bookMake) {
					// 板形成期間の取引(データ表示の際，期数を1始まりにするためtimeに1加えて渡す）
					doubleAuctionBookMake(time + 1, agentArray, market, data, output, this.i);
					time = this.updateTime();
				}
				else if (time <= this.time_end + 1){
					// 通常の取引
					// 20250327 全てのエージェントが1回だけ注文を出す
					if(agentArray[this.index] instanceof NormalAgent){ 
						doubleAuction(time + 1, agentArray, market, data, hft_flag, output, hft, this.i);
						time = this.updateTime();
					} else if(agentArray[this.index] instanceof FundamentalAgent) {
						// // 25/04/05 2回注文をチェックする (クラス内で工夫)
						doubleAuction(time + 1, agentArray, market, data, hft_flag, output, hft, this.i);
						doubleAuction(time + 1, agentArray, market, data, hft_flag, output, hft, this.i);
						time = this.updateTime();
					} else if(agentArray[this.index] instanceof TechnicalAgent) {
						// 25/04/05 2回注文をチェックする (クラス内で工夫)
						doubleAuction(time + 1, agentArray, market, data, hft_flag, output, hft, this.i);
						doubleAuction(time + 1, agentArray, market, data, hft_flag, output, hft, this.i);
						time = this.updateTime();
					}
				}
			}
			// 板寄せ方式
			else {
//				// 板形成期間の取引
//				itayoseBookMake(0, this.time_bookMake, agentArray, market, data);
//				// 通常の取引
//				itayose(this.time_bookMake, this.time_end, agentArray, market, data);
			}	
			// time = this.updateTime();
		}
		
		// ザラ場（連続ダブルオークション）方式
		// 21/12/31 名称変更time_book -> time_bookMake time渡し扱い変更
//		if(this.doubleAuction) {
//			// 板形成期間の取引
//			doubleAuctionBookMake(1, this.time_bookMake+1, agentArray, market, data, output);
//			// 通常の取引
//			doubleAuction(this.time_bookMake+1, this.time_end+1, agentArray, market, data, hft_flag, output, hft);
//		}
//		// 板寄せ方式
//		else {
//			// 板形成期間の取引
//			itayoseBookMake(0, this.time_bookMake, agentArray, market, data);
//			// 通常の取引
//			itayose(this.time_bookMake, this.time_end, agentArray, market, data);
//		}
		// スタイライズドファクトを表示
		// 21/5/5 変更 
//		data.outputStylizedFact(appNumber, param, appDir, output);
// 21/12/9　下記に変更するとエラーとなる．

		// 22/3/2 期間別注文数の集計

		if(param.getCnt_mrkt_ordr_strt() < param.getT_end()) {
			data.calcOrdersCount(
					market.getMarketPriceArray(),
					param.getT_bookMake(),
					param.getCnt_mrkt_ordr_strt(),
					param.getCnt_mrkt_ordr_strt() + param.getCnt_mrkt_ordr_prd(),
					param.getP_fund(),
					param.getd_pf(),
					param.getT_end());
		}

		// エージェントパフォーマンスをCSVに出力
		exportAgentPerformance();

		data.outputStylizedFact(appNumber, hft_flag, param, appDir, output, market.getMarketPriceArray(), time);
	}

	// ザラ場（連続ダブルオークション）方式
	// 21/12/31 timeの扱い変更
	// エージェントの型をNormalAgentからAgentに変更
	private void doubleAuction(int time, Agent[] agentArray, Market market, Data data, Flag.HFT hft_flag, boolean output, HFT hft, int i) {

		int j;
		// シミュレーション期間
		// 21/12/31
		//int time;
		// エージェント数
		int agentNum = agentArray.length;

		// 21/12/31
		//	for(time=startTime; time<endTime; time++) {

			// 21/5/21 HFTの注文処理を下に移動
			// 21/4/26 変更（boolean -> HFT_flag）
			//			if(hft_flag != HFT_flag.NONE) {
			//			// HFTの注文
			//				hft.order(market, time, hft_flag, param);
			//			}
			
			
			// 21/5/16 
			// この辺でDepthとTightnessはサンプリングすべきかも．
			
			
			// エージェントの添字
			j = time % agentNum;
			
			// エージェントがNormalAgentの場合のみ学習を実行 
			if (agentArray[j] instanceof NormalAgent) {
				((NormalAgent) agentArray[j]).updateWeight(market, time);
			}
			
			// エージェントの注文（AgentがNormalAgentかFundamentalAgentかTechnicalAgentかに応じて異なる実装が呼ばれる）
			Flag.Trade tradeFlag;
			if (agentArray[j] instanceof NormalAgent) {
				tradeFlag = ((NormalAgent) agentArray[j]).order(market, time, i);
			} else if (agentArray[j] instanceof FundamentalAgent) {
				tradeFlag = ((FundamentalAgent) agentArray[j]).order(market, time, i);
			} else if (agentArray[j] instanceof TechnicalAgent) {
				tradeFlag = ((TechnicalAgent) agentArray[j]).order(market, time, i);
			} else {
				tradeFlag = Flag.Trade.NONE;
			}
			
			// 注文成立の確認
			market.checkOrderBook(tradeFlag);

			
			// 有効期限の切れた板上注文の削除
			market.removeOrder(time);
			// 市場価格の時系列データを更新
			market.updateMarketPrice(time);

			// 21/5/21 HFT注文処理をここに移動
			if(hft_flag != Flag.HFT.NONE) {
			// HFTの注文
				hft.order(market, time, hft_flag, param);
			}

//			// 流動性指標の更新
			// 最後のパラメータをNormalAgentからAgentに変更
			data.update(market, agentArray, agentArray[j], time, param, hft_flag, hft);

			if(time % param.getDispInterval() == 0 && output) {
				// データの表示
				System.out.println("Time: " + time);
				data.dispData(market.getMarketPriceArray(), time);
				// 板の注文数の表示
//				market.dispBook();
				// 板情報の記録
//				csv.exportArrayCsv(bookFile, market.getAllBook());
				
				// 2024/07/17
				// 動作確認
				// System.out.println("instanceVolume" + market.getinstantVolume());
				// System.out.println("limitVolume" + market.getlimitVolume());
			}
			
			// 21/4/26 EXPORT_SPANをGetできるように変更
			// 21/5/8 出力期間も設定
			if(((time > csv.getExportStart()) && (time <= csv.getExportEnd(param.getT_end()))) && (time % csv.getExportSpan() == 0)) {
				// 確認用
//				if(output) {
//					System.out.println("Time: " + time);
//				}
				// CSVとしてデータを出力 - 修正: 一回だけ出力する
				// 21/12/31 time渡し 22/2/10 変更
				// 25/03/23 常にダミーのNormalAgentを使用してエラーを回避
				csv.exportCsv(time, data, market, param, hft, dummyNormalAgent);
			}
//		}
	}

	// ザラ場（連続ダブルオークション）方式：板形成期
//	private void doubleAuctionBookMake(int startTime, int endTime, NormalAgent[] agentArray, Market market, Data data, boolean output) {
	// エージェントの型をNormalAgentからAgentに変更
	private void doubleAuctionBookMake(int time, Agent[] agentArray, Market market, Data data, boolean output, int i) {
		int j;
		// シミュレーション期間
		// 21/12/31
		//int time;
		// エージェント数
		int agentNum = agentArray.length;
		
		// 21/12/31
		//for(time=startTime; time<endTime; time++) {
			// エージェントの添字
			j = time % agentNum;
			
			// エージェントがNormalAgentの場合のみ学習を実行 (一時的に無効に)
			if (agentArray[j] instanceof NormalAgent) {
				((NormalAgent) agentArray[j]).updateWeight(market, time);
			}
			
			// エージェントの注文（AgentがNormalAgentかFundamentalAgentかに応じて異なる実装が呼ばれる）
			Flag.Trade tradeFlag;
			if (agentArray[j] instanceof NormalAgent) {
				tradeFlag = ((NormalAgent) agentArray[j]).order(market, time, i);
			} else if (agentArray[j] instanceof FundamentalAgent) {
				// 板形成期に特有の処理をFundamentalAgentにも実装
				double orderPrice = market.getMarketPrice();
				tradeFlag = ((FundamentalAgent) agentArray[j]).bookMakeOrderType(orderPrice);
			} else if (agentArray[j] instanceof TechnicalAgent) {
				// 板形成期に特有の処理をTechnicalAgentにも実装
				double orderPrice = market.getMarketPrice();
				tradeFlag = ((TechnicalAgent) agentArray[j]).bookMakeOrderType(orderPrice);
			} else {
				tradeFlag = Flag.Trade.NONE;
			}
			
			// 市場価格の時系列データを更新
			market.updateMarketPrice(time);

			// 流動性指標の更新
			data.update(market, agentArray, agentArray[j], time, param, hft_flag, hft);

			if(output) {
				if(time % param.getDispInterval() == 0) {
					// データの表示
					data.dispData(market.getMarketPriceArray(), time);
					// 板の注文数の表示
	//				market.dispBook();
					// 板情報の記録
	//				csv.exportArrayCsv(bookFile, market.getAllBook());
	//			}
	//			if(time % 1000 == 0) {
					// 確認用
					System.out.println("Time: " + time);
					
					// 2024/07/17
					// 動作確認
					// System.out.println("instanceVolume" + market.getinstantVolume());
					// System.out.println("limitVolume" + market.getlimitVolume());
				}
			}
		//}
	}

	// 板寄せ方式
	private void itayose(int startTime, int endTime, NormalAgent[] agentArray, Market market, Data data) {
		// 未実装
		//
	}

	// 板寄せ方式の板形成
	private void itayoseBookMake(int startTime, int endTime, NormalAgent[] agentArray, Market market, Data data) {
		// 未実装
		//
	}

	// 21/06/24 試行全体の平均取得用 追加 星野
	public Data getData() {
		return this.data ;
	}
	
	// 21/12/31
	public int getTime() {
		return this.time;
	}
	public int updateTime() {
		this.time++;
		return this.time ;
	}

	public Flag.HFT getHft_flag() {
		return hft_flag;
	}
	
	// エージェントタイプごとの収益をCSVに出力するメソッド
	private void exportAgentPerformance() {
		try {
			// ファイルパスを指定
			// 20250325 とりあえず、今日の日付を指定
			java.io.File dirPath = new java.io.File("Result_CSV/" + "20250325");
			if (!dirPath.exists()) {
				dirPath.mkdirs(); // ディレクトリが存在しない場合は作成
			}
			
			java.io.File file = new java.io.File(dirPath, "agent_performance.csv");
			boolean isNewFile = !file.exists();
			
			// ファイルに書き込む準備
			java.io.FileWriter fileWriter = new java.io.FileWriter(file, true); // true = appendモード
			java.io.PrintWriter printWriter = new java.io.PrintWriter(new java.io.BufferedWriter(fileWriter));
			
			// 新規ファイルならヘッダーを書き込む
			if (isNewFile) {
				printWriter.println("Trial,HFTType,FundBuy,FundSell,FundProfit,FundBuyCount,FundSellCount,TechBuy,TechSell,TechProfit,TechBuyCount,TechSellCount");
			}
			
			// データを書き込む
			printWriter.printf("%d,%s,%.2f,%.2f,%.2f,%d,%d,%.2f,%.2f,%.2f,%d,%d%n",
				appNumber,
				hft_flag.toString(),
				market.getFundamentalAgentTotalBuy(),
				market.getFundamentalAgentTotalSell(),
				market.getFundamentalAgentProfit(),
				(int)market.getFundamentalAgentBuyCount(),
				(int)market.getFundamentalAgentSellCount(),
				market.getTechnicalAgentTotalBuy(),
				market.getTechnicalAgentTotalSell(),
				market.getTechnicalAgentProfit(),
				(int)market.getTechnicalAgentBuyCount(),
				(int)market.getTechnicalAgentSellCount()
			);
			// ファイルを閉じる
			printWriter.close();
			fileWriter.close();
			
			// 出力の確認（デバッグ用）
			if (output) {
				System.out.println("\n===================== エージェントパフォーマンス =====================");
				System.out.println("FundamentalAgentの収益: " + market.getFundamentalAgentProfit());
				System.out.println("FundamentalAgentの購入額合計: " + market.getFundamentalAgentTotalBuy());
				System.out.println("FundamentalAgentの売却額合計: " + market.getFundamentalAgentTotalSell());
				System.out.println("FundamentalAgentの買い注文回数: " + market.getFundamentalAgentBuyCount());
				System.out.println("FundamentalAgentの売り注文回数: " + market.getFundamentalAgentSellCount());
				System.out.println("TechnicalAgentの収益: " + market.getTechnicalAgentProfit());
				System.out.println("TechnicalAgentの購入額合計: " + market.getTechnicalAgentTotalBuy());
				System.out.println("TechnicalAgentの売却額合計: " + market.getTechnicalAgentTotalSell());
				System.out.println("TechnicalAgentの買い注文回数: " + market.getTechnicalAgentBuyCount());
				System.out.println("TechnicalAgentの売り注文回数: " + market.getTechnicalAgentSellCount());
				System.out.println("=================================================================\n");
			}
		} catch (java.io.IOException e) {
			e.printStackTrace();
			if (output) {
				System.out.println("エージェント収益のCSV出力でエラーが発生しました: " + e.getMessage());
			}
		}
	}

	public Parameter getParam() {
		return param;
	}

	public Market getMarket() {
		return market;
	}
}
