import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Data {
	// 流動性指標の時系列データ
	
	// 22/2/5 価格のサンプリングインターバル
	private int priceInterval ;
	// 22/2/5 騰落率（リターン）のサンプリングインターバル
	private int returnInterval ;

	// 期数
	private int[] turn;
//	// 市場価格
//	private double[] marketPrice;

	// HFTのポジション
	private int[] hftPosition;
	// 21/6/5 HFTの運用成績
	private double[] hftPerformance ;

///*	22/1/2 オーダーデプスインバランス戦略正規データ取得のためコメントアウト
	// 21/6/4 ポジション戦略ウェイト
	private double[] positionWeight ;
	// 21/5/16 オーダーインバランス戦略ウェイト
	private double[] orderImbalanceWeight ;
//*/	
	// 出来高
	private double[] volume;
	// １日あたりの出来高
	private double[] dayVolume;
	// ビッド・アスク・スプレッド
	private double[] tightness;
	// 値幅・出来高比率
	private double[] resiliency;
	// 最良気配値からの一定値までの買い注文数
	private int[] buyDepth;
	// 最良気配値からの一定値までの売り注文数
	private int[] sellDepth;

	
	// 騰落率
	private double[] rateofRise;
	// 騰落率の２乗
	private double[] square_rateofRise;
/*	22/3/9 メモリ使用削減
 	//	 エージェントの予想価格
	//	private Double[] expectedPriceList;
	// NAエージェントの注文価格
	private Double[] adjustedNAOrderPriceList;
	// 注文時の買いと売り
//	private TradeFlag[] tradeFlag;
	private Flag.Trade[] tradeFlag;
*/

	//以下追加実装：データ取得用リストの宣言
	// 買い板の注文数の総計
	private int[] amountBuyBook;
	// 売り板の注文数の総計
	private int[] amountSellBook;
	// 最良気配値からの一定値までの注文数
	private int[] depth;
	
/*	22/3/9 メモリ使用削減
	// 最良買い気配値
	private List<Double> bestBid;
	// 21/5/16 NAの最良買い気配値（bestBidがHFTのとき）
	private List<Double> bestBidNA ;
	// 最良売り気配値
	private List<Double> bestAsk;
	// 21/5/16 NAの最良売り気配値（bestAskがHFTのとき）
	private List<Double> bestAskNA ;
	
	// 21/5/13 追加
	// HFTエージェントのフェアバリュー
	private List<Double> hftFairValue;
	// HFTエージェントの買い注文価格
	private List<Double> hftBuyOrderPriceList;
	// HFTエージェントの売り注文価格
	private List<Double> hftSellOrderPriceList;
	// HFTエージェントのスプレッド
	private List<Double> hftSpreadList;
	//買いの取引価格
	private List<Double> execBuyOrderPriceList;
	//売りの取引価格
	private List<Double> execSellOrderPriceList;
*/
	//取引が成立した、買いの指値注文の注文主エージェント情報
	private String[] execBuyOrderAgent;
	//取引が成立した、売りの指値注文の注文主エージェント情報
	private String[] execSellOrderAgent;
	//取引が成立した、買いの成行注文の注文主エージェント情報
	private String[] execNewBuyOrderAgent;
	//取引が成立した、売りの成行注文の注文主エージェント情報
	private String[] execNewSellOrderAgent;
	// 21/6/2 HFTのNAV
	private int[] hftNAV;
	// 最良買い気配値の注文主エージェント情報
	private String[] bestBidOrderAgent;
	// 最良売り気配値の注文主エージェント情報
	private String[] bestAskOrderAgent;
	
	// 21/12/24 オーダーデプスインバランス検証用データ
	// オーダーデプスインバランスとリターンが正の相関
	//  （直前期） 買い（売り）Depth > 売り（買い）Depth かつ （当期）価格が上昇（下落）
	private int[] orderDepthImbalance_positive ;
	// オーダーデプスインバランスとリターンが負の相関
	//  （直前期） 買い（売り）Depth > 売り（買い）Depth かつ （当期）価格が下落（上昇）
	private int[] orderDepthImbalance_negative ;
	
	// 21/12/28 オーダーデプスインバランスの差分
	private double differenceODI;
	// 以上

	// 標準偏差
	private double std = 0.0;
	// 尖度
	private double kurt = 0.0;
	// ボラティリティクラスタリング
	private double[] vol_clus = null;

	// 取引が成立した注文板状のエージェント別の注文数
	private final Map<AggregationPhase, Map<ExecAgentType, Integer>> execOrdersByPeriod = new HashMap<>();

	// エージェント別最良気配値注文数
	private final Map<AggregationPhase, Map<ExecAgentType, Integer>> bestOrdersByPeriod = new HashMap<>();

	// エージェント別注文 集計期間
	public enum AggregationPhase {
		// ONE/第1期：急落前
		// TWO/第2期：NA成行売り注文時
		// THREE/第3期：NA成行売り注文終了時から底値
		// FOUR/第4期：底値から(ファンダ価格 - d_pf)達成時
		// FIVE/第5期：第4期終了から最後まで
		ONE("1"), TWO("2"), THREE("3"), FOUR("4"), FIVE("5");

		private final String name;

		AggregationPhase(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	// エージェント別注文 エージェントと注文種別
	public enum ExecAgentType {
		NA_BUY, NA_SELL, HFT_BUY, HFT_SELL
	}

	//	 エージェントの予想リターン
	//	private List<Double> expectedRet;
	//	 エージェントのファンダメンタル成分の重みw1
	//	private List<Double> w1;
	//	 エージェントのテクニカル成分の重みw2
	//	private List<Double> w2;
	//	 エージェントのファンダメンタル成分のリターンret1
	//	private List<Double> ret1;
	//	 エージェントのテクニカル成分のリターンret2
	//	private List<Double> ret2;


	public Data(int tEnd) {
		
		// 22/2/5　価格のサンプリングインターバル
		this.priceInterval= 100 ;
		// 22/2/5 騰落率（リターン）のサンプリングインターバル
		this.returnInterval= 100 ;

		// リストの初期化
		this.turn = new int[tEnd];
//		this.marketPrice = new double[tEnd];
		this.volume = new double[tEnd];
		this.dayVolume = new double[tEnd];
		this.tightness = new double[tEnd];
		this.resiliency = new double[tEnd];
		this.buyDepth = new int[tEnd];
		this.sellDepth = new int[tEnd];
		this.rateofRise = new double[tEnd];
		this.square_rateofRise = new double[tEnd];

		//	this.expectedRet = new ArrayList();
		//	this.expectedPriceList = new ArrayList();
/*	22/3/9 メモリ使用削減
		this.adjustedNAOrderPriceList = new ArrayList();
		this.tradeFlag = new ArrayFlag.Trade();
*/


//以下追加実装：データ取得用リストの初期化
		this.amountBuyBook = new int[tEnd];
		this.amountSellBook = new int[tEnd];
		this.depth = new int[tEnd];
/*	22/3/9 メモリ使用削減
 		this.bestBid = new ArrayList();
		// 21/5/16 
		this.bestBidNA = new ArrayList();
		this.bestAsk = new ArrayList();
		// 21/5/16 
		this.bestAskNA = new ArrayDouble[]() ;

		// 21/5/13
		this.hftFairValue = new ArrayList();
		this.hftBuyOrderPriceList = new ArrayList();
		this.hftSellOrderPriceList = new ArrayList();
		this.hftSpreadList = new ArrayList();
		this.execBuyOrderPriceList = new ArrayList();
		this.execSellOrderPriceList = new ArrayList();
*/
		this.execBuyOrderAgent = new String[tEnd];
		this.execSellOrderAgent = new String[tEnd];

		this.hftPosition = new int[tEnd];
///*22/1/2 オーダーデプスインバランス戦略正規データ取得のためコメントアウト
 		// 21/5/16
		this.positionWeight = new double[tEnd];
		this.orderImbalanceWeight = new double[tEnd];
		// 21/6/2
		this.hftNAV = new int[tEnd];
//*/		// 21/6/5
		this.hftPerformance = new double[tEnd];
///*	22/1/2 オーダーデプスインバランス戦略正規データ取得のためコメントアウト	
 		this.bestBidOrderAgent = new String[tEnd];
		this.bestAskOrderAgent = new String[tEnd];
		this.execNewBuyOrderAgent = new String[tEnd];
		this.execNewSellOrderAgent = new String[tEnd];
//*/		
		// 21/12/24
		this.orderDepthImbalance_positive = new int[tEnd];
		this.orderDepthImbalance_negative = new int[tEnd];

		// 21/12/28
		this.differenceODI = 0.0;
		
//以上

		//		this.w1 = new ArrayList();
		//		this.w2 = new ArrayList();
		//		this.ret1 = new ArrayList();
		//		this.ret2 = new ArrayList();

		// １日ごとのデータのみ初期値設定
		initDayData();
	}

	// 流動性指標の更新 21/4/26 変更（boolean -> HFT_flag）
	// Agent型に対応するため、シグネチャを変更
	public void update(Market market, Agent[] agentArray, Agent agent, int time, Parameter param, Flag.HFT hft_flag, HFT hft) {
		
		updateTurn(time);
		updateVolume(market, time);
		updateTightness(market, time);
		// 21/5/18
		updateDepth(time, market, param);
		//		updateDepth(market, hft_flag, hft);
//		updateMarketPrice(market, time);
		updateRateofRise(market, time);
		//		updateExpectedRet(agent);
		//		updateExpectedPrice(agent);
/*	22/3/9 メモリ使用削減
		// NormalAgentの場合のみ注文価格を更新
		if (agent instanceof NormalAgent) {
			updateAdjustedNAOrderPrice((NormalAgent)agent);
			updateFlag((NormalAgent)agent);
		}

//以下追加実装：出力用データの更新
		updateHFTFairValue(hft);
		updateAdjustedBuyOrderPrice(market);
		updateAdjustedSellOrderPrice(market);
		updateHFTSpread(market);
		updateBestBid(market);
		// 21/5/16
		updateBestBidNA(market);
		updateBestAsk(market);
		// 21/5/16
		updateBestAskNA(market);
//*/
		// 21/12/17
		updateAmountBuyBook(time, market, param);
		updateAmountSellBook(time, market, param);
/*	22/3/9 メモリ使用削減
		updateExecBuyOrderPrice(market);
		updateExecSellOrderPrice(market);
*/
		updateExecBuyOrderAgent(market, time);
		updateExecSellOrderAgent(market, time);
//*/
		updateHftPosition(market, time);
		// 21/6/5
		updateHftPerformance(hft,market, time);
///*22/1/2 オーダーデプスインバランス戦略正規データ取得のためコメントアウト
 		// 21/6/4
		updatePositionWeight(param, hft, time) ;
		updateOrderImbalanceWeight(market, time, param, hft);
		// 21/6/3
		updateHftNAV(market, time);
		updateBestBidOrderAgent(market, time);
		updateBestAskOrderAgent(market, time);
		updateNewExecBuyOrderAgent(market, time);
		updateNewExecSellOrderAgent(market, time);
//*/		
		// 21/12/24
		updateOrderDepthImbalancePositive(time, market, param);
		updateOrderDepthImbalanceNegative(time, market, param);

		
//以上


//		updateW1(agentArray);
//		updateW2(agentArray);
//		updateRet1(agent);
//		updateRet2(agent);
		// １日ごとの処理
		if(time % param.getDayPeriod() == 0) {
			updateDayVolume(market, time);
			updateResiliency(market, time);
			// 最大最小価格と出来高をリセット
			market.resetMarket_MaxMin();
			market.resetDayVolume();
		}
	}
	//期数を更新
	// 21/12/31
	private void updateTurn(int time) {
		this.turn[time] = time;
	}

	// 出来高の更新
	private void updateVolume(Market market, int time) {
	  this.volume[time] = market.getVolume();
	}


 	// １日あたりの出来高の更新
	private void updateDayVolume(Market market, int time) {
	  this.dayVolume[time] = market.getVolume();
	}


	// ビッド・アスク・スプレッドの更新
	private void updateTightness(Market market, int time) {
		// 最良売り価格
		double ask = market.getBestAsk();
		// 最良買い価格
		double bid = market.getBestBid();
		this.tightness[time] = (ask - bid);
	}

	// 22/2/9 
	// 値幅・出来高比率の計算
	private double calcResiliency(Market market, int time) {
		double res ;
    double dayVolume = this.dayVolume[time];
		if(dayVolume == 0) {
			// 値幅出来高比率
			res = 0;
		}
		else {
			// 市場価格の最大値と最小値
			double max = market.getMarketPriceMax();
			double min = market.getMarketPriceMin();
			// 値幅出来高比率の計算
			res = (max - min) / dayVolume;
		}
		
		return res ;
	}

	// 値幅・出来高比率の更新
	private void updateResiliency(Market market, int time) {
		double res ;
		double dayVolume = this.dayVolume[time];
		if(dayVolume == 0) {
			// 値幅出来高比率
			res = 0;
		}
		else {
			// 市場価格の最大値と最小値
			double max = market.getMarketPriceMax();
			double min = market.getMarketPriceMin();
			// 値幅出来高比率の計算
			res = (max - min) / dayVolume;
		}
		this.resiliency[time] = res;
	}

	// 最良気配値からの一定値までの注文数の更新
	// 21/12/17
	private void updateDepth(int time, Market market, Parameter param) {

		// 買いのDepth
		int currentBuyDepth = market.getBuyDepth();
		// 売りのDepth
		int currentSellDepth = market.getSellDepth();

		this.depth[time] = ((currentBuyDepth + currentSellDepth) / 2);
		
		// 見せ玉対応
		if (param.getSpoofer_flag() && (time > param.getSpoofer_strt())) {
			if(((time / param.getSpoofer_interval()) % 2) == 0) {
				currentBuyDepth += param.getNumberofspoofer();
			} else {
				currentSellDepth += param.getNumberofspoofer();
			}
		}

		this.buyDepth[time] = currentBuyDepth;
		this.sellDepth[time] = currentSellDepth;
	}
	
	// 2022/2/23 買いデプスの計算（見せ玉もカウントする）
	private int calcBuyDepth(int time, Market market, Parameter param) {

		// 買いのDepth
		int currentBuyDepth = market.getBuyDepth();

		// 見せ玉対応
		if (param.getSpoofer_flag() && (time > param.getSpoofer_strt())) {
			if(((time / param.getSpoofer_interval()) % 2) == 0) 
				currentBuyDepth += param.getNumberofspoofer();
		}
		
		return currentBuyDepth ;
	}
	
	// 2022/2/23 売りデプスの計算（見せ玉もカウントする）
	private int calcSellDepth(int time, Market market, Parameter param) {

		// 売りのDepth
		int currentSellDepth = market.getSellDepth();

		// 見せ玉対応
		if (param.getSpoofer_flag() && (time > param.getSpoofer_strt())) {
			if(((time / param.getSpoofer_interval()) % 2) != 0) 
				currentSellDepth += param.getNumberofspoofer();
		}

		return currentSellDepth ;
	}

///*22/1/2 オーダーデプスインバランス戦略正規データ取得のためコメントアウト
	// 21/5/16 ポジション戦略ウェイトの更新
	private void updatePositionWeight(Parameter param, HFT hft, int time) {
		this.positionWeight[time] = hft.getPositionWeight(param);
	}
	// 21/6/4 オーダーインバランス戦略ウェイトの更新
	// 21/12/16 期timeの追加
	private void updateOrderImbalanceWeight(Market market, int time, Parameter param, HFT hft) {
		this.orderImbalanceWeight[time] = hft.getOrderImbalanceWeight(market, time, param);
	}
//*/
	// 市場価格の更新
//	private void updateMarketPrice(Market market, int time) {
//		this.marketPrice[time] = market.getMarketPrice();
//	}

	// ボラティリティの更新
	private void updateRateofRise(Market market, int time) {
		// 直近の価格
		double p_t = market.getMarketPrice();
		// 22/2/5 n期前の価格　-> priceInteval に変更
		double pre_p_t = market.getPreMarketPrice(priceInterval, time);
//以下変更：騰落率を求める計算式を log で求めるよう修正
		// 騰落率
		double rr = Math.log(p_t/pre_p_t);
		this.rateofRise[time] = rr;
		this.square_rateofRise[time] = Math.pow(rr, 2);
	}

	// 22/2/5 リターン（騰落率）を計算する
	private double calcReturn(Market market, int time) {
		// 直近の価格
		double p_t = market.getMarketPrice();
		// 22/2/5 n期前の価格　-> priceInteval に変更
		double pre_p_t = market.getPreMarketPrice(priceInterval, time);
		//以下変更：騰落率を求める計算式を log で求めるよう修正
		// 騰落率
		double rr = Math.log(p_t/pre_p_t);
		
		return rr ;
	}

	// 22/2/5 リターン（騰落率）の2乗を計算する．
	private double calcSquareReturn(Market market, int time) {
		 double rr = calcReturn(market, time) ;
		 
		 return Math.pow(rr, 2);
	}
	
	// 取引が成立した、買いの指値注文の注文主エージェント情報の更新
	private void updateExecBuyOrderAgent(Market market, int time) {
		this.execBuyOrderAgent[time] = market.getExecBuyOrderAgent();
	}

	// 取引が成立した、売りの指値注文の注文主エージェント情報の更新
	private void updateExecSellOrderAgent(Market market, int time) {
		this.execSellOrderAgent[time] = market.getExecSellOrderAgent();
	}
	//買い板の注文数の総計の更新
	// 21/12/17
	private void updateAmountBuyBook(int time, Market market, Parameter param) {
		
		int amount = 0 ;
		
		// 見せ玉対応
		if (param.getSpoofer_flag() && (time > param.getSpoofer_strt()) && (((time / param.getSpoofer_interval()) % 2) == 0)) 
			amount = market.getAmountBuyBook() + param.getNumberofspoofer();
		else 
			amount = market.getAmountBuyBook();
		
		this.amountBuyBook[time] = amount;
	}
	
	//売り板の注文数の総計の更新
	// 21/12/17
	private void updateAmountSellBook(int time, Market market, Parameter param) {
		int amount = 0 ;
		
		// 見せ玉対応
		if (param.getSpoofer_flag() && (time > param.getSpoofer_strt()) && (((time / param.getSpoofer_interval()) % 2) == 1)) 
			amount = market.getAmountSellBook() + param.getNumberofspoofer();
		else 
			amount = market.getAmountSellBook();
		
		this.amountSellBook[time] = amount;
	}

	//買い板の注文数の総計の計算
	// 22/2/5
	private int calcAmountOfBuyBook(int time, Market market, Parameter param) {
		
		int amount = 0 ;
		
		// 見せ玉対応
		if (param.getSpoofer_flag() && (time > param.getSpoofer_strt()) && (((time / param.getSpoofer_interval()) % 2) == 0)) 
			amount = market.getAmountBuyBook() + param.getNumberofspoofer();
		else 
			amount = market.getAmountBuyBook();
		
		return amount;
	}
	
	//売り板の注文数の総計の計算
	// 22/2/5
	private int calcAmountOfSellBook(int time, Market market, Parameter param) {
		int amount = 0 ;
		
		// 見せ玉対応
		if (param.getSpoofer_flag() && (time > param.getSpoofer_strt()) && (((time / param.getSpoofer_interval()) % 2) == 1)) 
			amount = market.getAmountSellBook() + param.getNumberofspoofer();
		else 
			amount = market.getAmountSellBook();
		
		return amount;
	}

	//HFTのポジションの更新
	private void updateHftPosition(Market market, int time) {
		this.hftPosition[time] = market.getHftPosition();
	}
	
	// 21/6/5
	private void updateHftPerformance(HFT hft, Market market, int time) {
		this.hftPerformance[time] = hft.getHFTperformance(market) ;
	}
///*22/1/2 オーダーデプスインバランス戦略正規データ取得のためコメントアウト
	// 21/6/2 HFTのNAVの更新
	private void updateHftNAV(Market market, int time) {
		this.hftNAV[time] = market.getHftNetAssetValue();
	}

	//最良買い気配値の注文主エージェント情報の更新
	private void updateBestBidOrderAgent(Market market, int time) {
		this.bestBidOrderAgent[time] = market.getBestBidAgent();
	}

	//最良売り気配値の注文主エージェント情報の更新
	private void updateBestAskOrderAgent(Market market, int time) {
		this.bestAskOrderAgent[time] = market.getBestAskAgent();
	}

	// 22/2/5 
	//最良買い気配値の注文主エージェントの特定
	private int calcBestBidAgent(Market market) {
		
		String agentType = market.getBestBidAgent();
		
		if (agentType == "HFTAGENT") return 2;
		else if (agentType == "NORMAL") return 1;
		else if (agentType == "NOTHING") return 0 ;
		else return 9;
	}

	// 22/2/5 
	//最良売り気配値の注文主エージェントの特定
	private int calcBestAskAgent(Market market) {
		
		String agentType = market.getBestAskAgent();
		
		if (agentType == "HFTAGENT") return 2;
		else if (agentType == "NORMAL") return 1;
		else if (agentType == "NOTHING") return 0 ;
		else return 9;
	}

	// 22/2/5 
	//板上（市場）の買い注文主エージェントの特定
	private int calcBuyOrderAgent(Market market) {
		
		String agentType = market.getExecBuyOrderAgent();
		
		if (agentType == "HFTAGENT") return 2;
		else if (agentType == "NORMAL") return 1;
		else if (agentType == "NOTHING") return 0 ;
		else return 9;
	}

	// 22/2/5 
	//板上（市場）の売り注文主エージェントの特定
	private int calcSellOrderAgent(Market market) {
		
		String agentType = market.getExecSellOrderAgent();
		
		if (agentType == "HFTAGENT") return 2;
		else if (agentType == "NORMAL") return 1;
		else if (agentType == "NOTHING") return 0 ;
		else return 9;
	}
	
	// 22/2/5 
	//板上（市場）の買い注文主エージェントの特定
	private int calcNewBuyOrderAgent(Market market) {
		
		String agentType = market.getNewExecBuyOrderAgent();
		
		if (agentType == "HFTAGENT") return 2;
		else if (agentType == "NORMAL") return 1;
		else if (agentType == "NOTHING") return 0 ;
		else return 9;
	}

	// 22/2/5 
	//板上（市場）の売り注文主エージェントの特定
	private int calcNewSellOrderAgent(Market market) {
		
		String agentType = market.getNewExecSellOrderAgent();
		
		if (agentType == "HFTAGENT") return 2;
		else if (agentType == "NORMAL") return 1;
		else if (agentType == "NOTHING") return 0 ;
		else return 9;
	}
	
	//取引が成立した、買いの成行注文の注文主エージェント情報の更新
	private void updateNewExecBuyOrderAgent(Market market, int time) {
		this.execNewBuyOrderAgent[time] = market.getNewExecBuyOrderAgent();
	}

	//取引が成立した、売りの成行注文の注文主エージェント情報
	private void updateNewExecSellOrderAgent(Market market, int time) {
		this.execNewSellOrderAgent[time] = market.getNewExecSellOrderAgent();
	}
//*/	
	

	// 21/12/24
	// ここから
	private void updateOrderDepthImbalancePositive(int time, Market market, Parameter param) {

		// 直前期の買いのDepth
		int preBuyDepth = this.getPreBuyDepth(time);
		// 直前期の売りのDepth
		int preSellDepth = this.getPreSellDepth(time);
		// 直前期の約定価格
		double preMarketPrice = market.getPreMarketPrice(1, time);
		// 当期の約定価格
		double MarketPrice = market.getMarketPrice();

		if (((preBuyDepth > preSellDepth) && (preMarketPrice < MarketPrice)) ||	((preBuyDepth < preSellDepth) && (preMarketPrice > MarketPrice)))
			this.orderDepthImbalance_positive[time] = 1;
		else 
			this.orderDepthImbalance_positive[time] = 0;
	}
	
	private void updateOrderDepthImbalanceNegative(int time, Market market, Parameter param) {

		// 直前期の買いのDepth
		int preBuyDepth = this.getPreBuyDepth(time);
		// 直前期の売りのDepth
		int preSellDepth = this.getPreSellDepth(time);
		// 直前期の約定価格
		double preMarketPrice = market.getPreMarketPrice(1, time);
		// 当期の約定価格
		double MarketPrice = market.getMarketPrice();

		if (((preBuyDepth > preSellDepth) && (preMarketPrice > MarketPrice)) || ((preBuyDepth < preSellDepth) && (preMarketPrice < MarketPrice)))
			this.orderDepthImbalance_negative[time] = 1;
		else 
			this.orderDepthImbalance_negative[time] = 0;
	}

	// 21/12/24
	private int calcOrderDepthImbalancePositive(int time, Market market, Parameter param) {

		// 直前期の買いのDepth
		int preBuyDepth = this.getPreBuyDepth(time);
		// 直前期の売りのDepth
		int preSellDepth = this.getPreSellDepth(time);
		// 直前期の約定価格
		double preMarketPrice = market.getPreMarketPrice(1, time);
		// 当期の約定価格
		double MarketPrice = market.getMarketPrice();

		if (((preBuyDepth > preSellDepth) && (preMarketPrice < MarketPrice)) ||	((preBuyDepth < preSellDepth) && (preMarketPrice > MarketPrice)))
			return 1 ;
		else 
			return 0 ;
	}
	
	private int calcOrderDepthImbalanceNegative(int time, Market market, Parameter param) {

		// 直前期の買いのDepth
		int preBuyDepth = this.getPreBuyDepth(time);
		// 直前期の売りのDepth
		int preSellDepth = this.getPreSellDepth(time);
		// 直前期の約定価格
		double preMarketPrice = market.getPreMarketPrice(1, time);
		// 当期の約定価格
		double MarketPrice = market.getMarketPrice();

		if (((preBuyDepth > preSellDepth) && (preMarketPrice > MarketPrice)) || ((preBuyDepth < preSellDepth) && (preMarketPrice < MarketPrice)))
			return 1 ;
		else 
			return 0 ;
		}


//以上

	//	// ファンダメンタル成分の重みの更新
	//	private void updateW1(NormalAgent[] agentArray) {
	//		Stream<NormalAgent> agentStream = Arrays.stream(agentArray);
	//		// 全エージェントのファンダメンタル成分の重みの平均
	//		double w = agentStream.mapToDouble(a -> a.getW1()).average().getAsDouble();
	//		this.w1.add(w);
	//	}
	//
	//	 テクニカル成分の重みの更新
	//	private void updateW2(NormalAgent[] agentArray) {
	//		Stream<NormalAgent> agentStream = Arrays.stream(agentArray);
	//		// 全エージェントのテクニカル成分の重みの平均
	//		double w = agentStream.mapToDouble(a -> a.getW2()).average().getAsDouble();
	//		this.w2.add(w);
	//	}
	//
	//	// ファンダメンタル成分の重みの更新
	//	private void updateRet1(NormalAgent agent) {
	//		this.ret1.add(agent.getRet1());
	//	}
	//
	//	// テクニカル成分の重みの更新
	//	private void updateRet2(NormalAgent agent) {
	//		this.ret2.add(agent.getRet2());
	//	}

	// スタイライズドファクトを表示
	// 21/5/5 変更
//	public void outputStylizedFact(int appNumber, Parameter param, String appDirPath, boolean output) {
// 21/12/9 下記に変更するとエラーが出る．
	public void outputStylizedFact(int appNumber, Flag.HFT hft_flag, Parameter param, String appDirPath, boolean output, double[] marketPrice, int time) {

		// 標準偏差
		std = calcStd(param, time);
		// 尖度
		kurt = calcKurtosis(std, param, time);
		// ボラティリティクラスタリング
		vol_clus = calcVolatilityClustering(param);

		// 21/12/9 追加
		String agent_type = hft_flag.toString();
		
		if(output) {
			dispStylizedFact(kurt, vol_clus);
		}
		
		// csv出力
		String separator = File.separator;
		// "Result_CSV/YYYYMMDD_HHMM/XX/statistic_data.csv"

		// 21/5/5 変更
//		 String fileName = appDirPath + separator + "app" + appNumber + "_statistic_data.csv";
// 21/12/9 下記に変更するとエラーが出る．
		String fileName = appDirPath + separator + appNumber + "_" + agent_type + "_statistic_data.csv";
		exportStylizedFact(fileName, param, std, kurt, vol_clus, marketPrice, time);
	}

	// スタイライズドファクトを表示
	private void dispStylizedFact(double kurt, double[] volatility) {
		System.out.println("Stylized fact");
		System.out.println("====================================");
		// 尖度
		System.out.println("Kurtosis    : " + (kurt));
		// ボラティリティクラスタリング
		int lag = 5;
		for(int i=0; i<lag+1; i++) {
			System.out.println("volatility " + i + ": " + volatility[i]);
		}
		System.out.println("====================================\n");
	}

	// スタイライズドファクトのcsv出力
	private void exportStylizedFact(String fileName, Parameter param, double std, double kurt, double[] volatility, double[] marketPrice, int time) {
		FileWriter f;
		PrintWriter p;
		// 21/12/24
		//statData = new double[8];
		double[] statData = new double[11];
		
		statData[0] = volume[time];
    statData[1] = Arrays.stream(Arrays.copyOfRange(tightness, param.getT_bookMake(), time)).average().orElse(0.0);
    statData[2] = Arrays.stream(resiliency).average().orElse(0.0);
		statData[3] = Arrays.stream(Arrays.copyOfRange(buyDepth, param.getT_bookMake(), time)).average().orElse(0.0);
		statData[4] = Arrays.stream(Arrays.copyOfRange(sellDepth, param.getT_bookMake(), time)).average().orElse(0.0);
		statData[5] = Arrays.stream(Arrays.copyOfRange(depth, param.getT_bookMake(), time)).average().orElse(0.0);
		statData[6] = std;
		statData[7] = Arrays.stream(Arrays.copyOfRange(marketPrice, param.getT_bookMake(), time)).average().orElse(0.0);
		// 21/12/24
		int sum_pODI = Arrays.stream(orderDepthImbalance_positive).sum();
		int sum_nODI = Arrays.stream(orderDepthImbalance_negative).sum();
		statData[8] = (double)sum_pODI/statData[0];
		statData[9] = (double)sum_nODI/statData[0];
		statData[10] = (double)statData[8] - statData[9];
		// 21/12/28
		differenceODI = statData[10];
		
		try {
			f = new FileWriter(fileName, false);
	    	p = new PrintWriter(new BufferedWriter(f));

	    	// 21/4/29 コメントアウト
//	    	// 表形式用出力データ
//			for(int i=0; i<6; i++) {
//				if(i != 4) {
//					p.print(statData[i] + ",");
//				}
//			}
//			p.println("\n");

	    	// スタイライズドファクト
			p.println("stylized fact:");
	    	p.println("std," + std);
	    	p.println("kurt," + kurt);

	    	int lag = 5;
			for(int i=0; i<lag+1; i++) {
				p.println("lag " + i + "," + volatility[i]);
			}
			// 統計データ
			p.println("\nvolume," + statData[0]);
			p.println("\naverage:");
			p.println("tightness," + statData[1]);
			p.println("resiliency," + statData[2]);
			p.println("buydepth," + statData[3]);
			p.println("selldepth," + statData[4]);
			p.println("depth," + statData[5]);
			p.println("marketPrice," + statData[7]);
			// 21/12/24
			p.println("\n");
			p.println("pODIratio," + statData[8]);
			p.println("nODIratio," + statData[9]);
			p.println("differenceODI," + statData[10]);

			// エージェントごとの期間別注文数
			if(param.getCnt_mrkt_ordr_strt() < param.getT_end()) {

				p.println();
				p.println("Number of orders in the order book by agent types that matched to a new order");
				p.println(",,NA(count),NA(rate),HFT(count),HFT(rate)");
				for (AggregationPhase phase : AggregationPhase.values()) {
					// 買い
					double naBuy = execOrdersByPeriod.get(phase).get(ExecAgentType.NA_BUY);
					double hftBuy = execOrdersByPeriod.get(phase).get(ExecAgentType.HFT_BUY);
					double totalBuy = naBuy + hftBuy;
					p.println(String.format("Phase %s,buy,%f,%f,%f,%f", phase, naBuy, naBuy / totalBuy, hftBuy, hftBuy / totalBuy));
					// 売り
					double naSell = execOrdersByPeriod.get(phase).get(ExecAgentType.NA_SELL);
					double hftSell = execOrdersByPeriod.get(phase).get(ExecAgentType.HFT_SELL);
					double totalSell = naSell + hftSell;
					p.println(String.format(",sell,%f,%f,%f,%f", naSell, naSell / totalSell, hftSell, hftSell / totalSell));
				}

				// エージェントごとの期間別最良気配注文数
				p.println();
				p.println(" Number of best bid/ask orders in the order book by agent types");
				p.println(",,NA(count),NA(rate),HFT(count),HFT(rate)");
				for (AggregationPhase phase : AggregationPhase.values()) {
					// 買い
					double naBuy = bestOrdersByPeriod.get(phase).get(ExecAgentType.NA_BUY);
					double hftBuy = bestOrdersByPeriod.get(phase).get(ExecAgentType.HFT_BUY);
					double totalBuy = naBuy + hftBuy;
					p.println(String.format("Phase %s,buy,%f,%f,%f,%f", phase, naBuy, naBuy / totalBuy, hftBuy, hftBuy / totalBuy));
					// 売り
					double naSell = bestOrdersByPeriod.get(phase).get(ExecAgentType.NA_SELL);
					double hftSell = bestOrdersByPeriod.get(phase).get(ExecAgentType.HFT_SELL);
					double totalSell = naSell + hftSell;
					p.println(String.format(",sell,%f,%f,%f,%f", naSell, naSell / totalSell, hftSell, hftSell / totalSell));
				}
			}

			// 書き込み終了
			p.close();
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 標準偏差の計算
	public double calcStd(Parameter param, int time) {

//以下追加実装:騰落率のデータを100件ごとの騰落率のリストから取得するように変更
		// 板形成期間を除いたリスト
		double[] srr = Arrays.copyOfRange(rateofRise, param.getT_bookMake(), time);
		// n件ごとの騰落率のリスト
		List<Double> rr = new ArrayList<>();
		// 22/2/5 騰落率のリストをn件ごとに取得 -> returnInterval へ変更
//		int n=100;
		double y =0;
		for(int k = 0; k<srr.length - 1; k++) {
			if(k % returnInterval == 0){
				y = srr[k];
				rr.add(y);
			}
		}
//以上

		// 平均
		double mean = rr.stream().mapToDouble(m -> m).average().orElse(0.0);
		// 分散
		double variance = rr.stream().mapToDouble(x -> Math.pow(x - mean, 2)).sum() / rr.size();
		// 標準偏差
		double std = Math.sqrt(variance);
		return std;
	}

	// 尖度の計算
	public double calcKurtosis(double std, Parameter param, int time) {

//以下追加実装:尖度を100件ごとの騰落率のリストから取得するように変更
		// 板形成期間を除いたリスト
		double[] srr = Arrays.copyOfRange(rateofRise, param.getT_bookMake(), time);
		// n件ごとの騰落率のリスト
		List<Double> rr = new ArrayList<>();
		// 22/2/5 騰落率のリストをn件ごとに取得 -> returnInterval へ変更
		//		int n=100;
		double y=0;
		for(int k=0; k<srr.length; k++) {
			if(k% returnInterval == 0){
				y=srr[k];
				rr.add(y);
			}
		}
//以上

		// 平均
		double mean = rr.stream().mapToDouble(m -> m).average().orElse(0.0);
		// 尖度
		double kurt = rr.stream().mapToDouble(x -> Math.pow((x - mean) / std, 4)).sum();
		kurt = kurt / rr.size();
		return kurt;
	}

	// ボラティリティクラスタリングの計算
	public double[] calcVolatilityClustering(Parameter param) {
		int lag = 5+1;

//以下追加実装:ボラティリティクラスタリングを100件ごとの騰落率のリストから取得するように変更
		// 板形成期間を除いたリスト
		// double[] を List<Double>に変換
		List<Double> squareRateofRise = Arrays.stream(square_rateofRise).boxed().collect(Collectors.toList());;
		List<Double> srr2 = squareRateofRise.subList(param.getT_bookMake(), squareRateofRise.size());
		double[] sum_square_rateofRise = new double[lag];
		// lag1からlag5
		double[] sum_square_rateofRise_lag = new double[lag];
		double[] z = new double[lag];
		// n件ごとの騰落率のリスト
		List<Double> rr2 = new ArrayList<Double>();
		// 22/2/5 騰落率のリストをn件ごとに取得 -> returnInterval　に変更
		// int n=100;
		double y=0;
		for(int k=0; k<srr2.size(); k++) {
			if(k% returnInterval ==0){
				y =srr2.get(k);
				rr2.add(y);
			}
		}
		double length = rr2.size();
//以上

		// ボラティリティ
		double[] volatility = new double[lag];
		for(int lag_i=1; lag_i<=lag; lag_i++) {
			sum_square_rateofRise[lag_i-1] = rr2.stream().mapToDouble(x -> x).sum() / (length - lag_i);
			sum_square_rateofRise_lag[lag_i-1] = rr2.subList(lag_i, rr2.size()).stream().mapToDouble(x -> x).sum() / (length - lag_i);
			// 配列を0で初期化
			Arrays.fill(z, 0);
			for(int j=0; j<=length-lag_i; j++) {
				z[0] += (rr2.get(j) - sum_square_rateofRise[lag_i-1]) * (rr2.get(j + lag_i-1) - sum_square_rateofRise_lag[lag_i-1]);
				z[1] += Math.pow((rr2.get(j) - sum_square_rateofRise[lag_i-1]), 2);
				z[2] += Math.pow((rr2.get(j + lag_i-1) - sum_square_rateofRise_lag[lag_i-1]), 2);
			}
			volatility[lag_i-1] = z[0] / Math.sqrt(z[1] * z[2]);
		}
		return volatility;
	}

	// １日ごとのデータを初期化
	private void initDayData() {
		this.dayVolume[0] = 0.0;
		this.resiliency[0] = 0.0;
	}

	public void dispData(double[] marketPrice, int time) {
		System.out.println("====================================");
		System.out.println("Volume    : " + this.volume[time]);
		System.out.println("Tightness : " + this.tightness[time]);
		System.out.println("Resiliency: " + this.resiliency[time]);
		System.out.println("Buy_Depth : " + this.buyDepth[time]);
		System.out.println("Sell_Depth: " + this.sellDepth[time]);
		System.out.println("Depth     : " + this.depth[time]);
		System.out.println("Price     : " + marketPrice[time]);
		System.out.println("Rate_Rise : " + this.rateofRise[time]);
//		System.out.println("W1        : " + this.w1.get(this.w1.size()-1));
//		System.out.println("W2        : " + this.w2.get(this.w2.size()-1));
		System.out.println("====================================\n");
	}
	
	// 21/12/24
	// 約定直近期の買いDepthを得る
	public int getPreBuyDepth(int time) {
		
		int preBuyDepth = 0 ;

		if (time > 2) preBuyDepth = this.buyDepth[time - 1];
		else preBuyDepth = this.buyDepth[0];
		
		return preBuyDepth ;
		
	}
	// 約定直近期の売りDepthを得る
	public int getPreSellDepth(int time) {
		
		int preSellDepth = 0 ;

		if (time > 2) preSellDepth = this.sellDepth[time - 1];
		else preSellDepth = this.sellDepth[0];
		
		return preSellDepth ;
		
	}
	// 21/12/28 
	public double getDifferenceODI() {
		return differenceODI ;
	}
	
	// 21/06/24 HFTのパフォーマンス取得用 追加 星野
	public double[] getHftPerformance() {
		return hftPerformance;
	}

	// 21/06/24 HFTのポジション取得用 追加 星野
	public int[] getHftPosition() {
		return hftPosition;
	}
	
	// 21/12/29 exportするデータの属性（オーダーデプスインバランスとリターンの相関性）
	public List<String> getAttributeListforDifferenceODI() {
		
		List<String> dataset = new ArrayList<>();
		
		dataset.add("Turn");
		dataset.add("MarketPrice");
		// 2024/09/03 HFTは使用しないためコメントアウト
		// dataset.add("HFTPosition");
		// dataset.add("HFTPerformance");
		dataset.add("BuyDepth");
		dataset.add("SellDepth");
		dataset.add("Volume");
		
		// 2024/07/17 成行注文と指値注文の注文数
		/*
		dataset.add("instantVolume");
		dataset.add("limitVolume");
		*/
		
		// 2024/07/24 成行注文と指値注文が変更されたケース
		/*
		dataset.add("LimitToInstantVolume");
		dataset.add("InstantToLimitVolume");
		*/

		// 2024/09/03 Tightness, Depthを追加
		/*
		dataset.add("Tightness");
		dataset.add("Depth");
		*/

		return dataset;
	}
		
	// 21/12/29 exportするデータ（オーダーデプスインバランスとリターンの相関性）
	public List<Integer> getListforDifferenceODI(int time, Market market, HFT hft) {

    List<Integer> dataset = new ArrayList<>();
		
		dataset.add(time);
		dataset.add((int)market.getMarketPrice());
		// 2024/09/03 HFTは使用しないためコメントアウト
		// dataset.add(market.getHftPosition());
		// dataset.add((int)hft.getHFTperformance(market));
		dataset.add(market.getBuyDepth());
		dataset.add(market.getSellDepth());
		dataset.add((int)market.getVolume());
		
		// 2024/07/17 成行注文と指値注文の注文数
		/*
		dataset.add((int)market.getinstantVolume());
		dataset.add((int)market.getlimitVolume());
		*/

		// 2024/07/24 成行注文と指値注文が変更されたケース
		/*
		dataset.add((int)market.getLimitToInstantVolume());
		dataset.add((int)market.getInstantToLimitVolume());
		*/		

		// 2024/09/03 Tightness, Depthを追加
		/*
		dataset.add(getTightness());
		dataset.add(getDepth());		
		*/

		return dataset;
	}

	// 22/2/4 すべてのデータをexportする
	public List<String> getAttributeListforFullData() {
		
		List<String> dataset = new ArrayList<>();
		
		dataset.add("Turn");
		dataset.add("MarketPrice");
		dataset.add("Return");	// rateofRiseに対応
		dataset.add("SquareReturn");	// square_rateofRiseに対応"
		dataset.add("AmountOfBuyBook");	
		dataset.add("AmountOfSellBook");

/*	22/3/9 メモリ使用削減		
		dataset.add("BestBid");
		dataset.add("BestBidOfNA");	// bestBidNAに対応
*/
		dataset.add("BestBidOrderAgent");	// bestBidOrderAgentに対応

/*	22/3/9 メモリ使用削減		
		dataset.add("BestAsk");
		dataset.add("BestAskOfNA");	// bestAskNAに対応
*/
		dataset.add("BestAskOrderAgent");	// bestBidOrderAgentに対応
		
/*	22/3/9 メモリ使用削減		
		dataset.add("ExecBuyOrderPrice");	// execBuyOrderPriceListに対応
		dataset.add("ExecSellOrderPrice");	// execSellOrderPriceListに対応
*/
		dataset.add("ExecBuyOrderAgent");	// execBuyOrderAgentに対応
		dataset.add("ExecSellOrderAgent");	// execSellOrderAgentに対応
		dataset.add("ExecNewBuyOrderAgent");	// execNewBuyOrderAgentに対応
		dataset.add("ExecNewSellOrderAgent");	// execNewSellBuyOrderAgentに対応
		
		
		// 流動性（ここで毎期出力する意味はあるのだろうか？）
		dataset.add("Volume");
		dataset.add("DayVolume");
		dataset.add("Tightness");
		dataset.add("Resiliency");
		dataset.add("Depth");
		dataset.add("BuyDepth");
		dataset.add("SellDepth");

/*	22/3/9 メモリ使用削減		
		// NA
		dataset.add("ExpectedPriceOfNA");	// expectedPriceListに対応
		dataset.add("OrderPirceOfNA");	// adjustedNAOrderPriceListに対応
		dataset.add("OrderTypeOfNA");	// tradeFlagに対応
*/
		// HFT
		dataset.add("HFTPosition");
		dataset.add("HFTPerformance");
/*	22/3/9 メモリ使用削減		
		// HFT ポジション影響度
		dataset.add("PositionWeight");
		dataset.add("OrderBookImbalanceWeight");
		dataset.add("HFTFairValue");
		dataset.add("BuyOrderPriceOfHFT");	// hftBuyOrderPriceListに対応
		dataset.add("SellOrderPriceOfHFT");	// hftSellOrderPriceListに対応
		dataset.add("SpreadOfHFT");	// hftSpreadListに対応
*/
		// オーダーブックインバランス
		// PositiveOrderBookImbalanceとNegativeOrderBookImbalanceは，試行終了後にDifferenceODIを計算するために必要な値だが
		// (exportStylizedFactのdifferenceODI参照），デバッグのため取得しておく．<- orderDepthImbalance_positiveとorderDepthImbalance_negativeと二重持ちせざるを得ない？
		dataset.add("PositiveOrderBookImbalance");	// orderDepthImbalance_positiveに対応
		dataset.add("NegativeOrderBookImbalance");	// orderDepthImbalance_negativeに対応
		// dataset.add("DifferenceODI");	// differenceODIに対応
		
		return dataset;
	}
		
	// 22/2/31/12/29 exportするデータ（オーダーデプスインバランスとリターンの相関性）
	public List<Object> getListforFullData(int time, Market market, Parameter param, HFT hft, NormalAgent nagent) {
		
		List<Object> dataset = new ArrayList<>();
		
//		dataset.add("Turn");
//		dataset.add("MarketPrice");
//		dataset.add("Return");	// rateofRiseに対応
//		dataset.add("SquareReturn");	// square_rateofRiseに対応"
//		dataset.add("AmountOfBuyBook");	
//		dataset.add("AmountOfSellBook");

		dataset.add(time);				// doubleではなくint型がよい
		dataset.add(market.getMarketPrice());
		dataset.add(calcReturn(market, time));
		dataset.add(calcSquareReturn(market, time));
		dataset.add(calcAmountOfBuyBook(time, market, param));// doubleではなくint型がよい
		dataset.add(calcAmountOfSellBook(time, market, param));// doubleではなくint型がよい
		
//		dataset.add("BestBid");
//		dataset.add("BestBidOfNA");	// bestBidNAに対応
//		dataset.add("BestBidOrderAgent");	// bestBidOrderAgentに対応
//		dataset.add("BestAsk");
//		dataset.add("BestAskOfNA");	// bestAskNAに対応
//		dataset.add("BestAskOrderAgent");	// bestBidOrderAgentに対応

/*	22/3/9 メモリ使用削減	
 		dataset.add(market.getBestBid());
		dataset.add(market.getBestBidNA());
*/
		dataset.add(market.getBestBidAgent());	// doubleではなくStringがよい
/*	22/3/9 メモリ使用削減	
		dataset.add(market.getBestAsk());
		dataset.add(market.getBestAskNA());
*/
		dataset.add(market.getBestAskAgent());	// doubleではなくStringがよい
		
		
//		dataset.add("ExecBuyOrderPrice");	// execBuyOrderPriceListに対応
//		dataset.add("ExecSellOrderPrice");	// execSellOrderPriceListに対応
//		dataset.add("ExecBuyOrderAgent");	// execBuyOrderAgentに対応
//		dataset.add("ExecSellOrderAgent");	// execSellOrderAgentに対応
//		dataset.add("ExecNewBuyOrderAgent");	// execNewBuyOrderAgentに対応
//		dataset.add("ExecNewSellOrderAgent");	// execNewSellBuyOrderAgentに対応

/*	22/3/9 メモリ使用削減	
		dataset.add(market.getExecBuyOrderPrice());
		dataset.add(market.getExecSellOrderPrice());
*/
		dataset.add(market.getExecBuyOrderAgent());// doubleではなくStringがよい
		dataset.add(market.getExecSellOrderAgent());// doubleではなくStringがよい
		dataset.add(market.getNewExecBuyOrderAgent());// doubleではなくStringがよい
		dataset.add(market.getNewExecSellOrderAgent());// doubleではなくStringがよい
		
//		
//		// 流動性（ここで毎期出力する意味はあるのだろうか？）
//		dataset.add("Volume");
//		dataset.add("DayVolume");
//		dataset.add("Tightness");
//		dataset.add("Resiliency");
//		dataset.add("Depth");
//		dataset.add("BuyDepth");
//		dataset.add("SellDepth");
		
		dataset.add(market.getVolume());
		dataset.add(market.getDayVolume());
		dataset.add(market.getBestAsk()- market.getBestBid());
		dataset.add(calcResiliency(market, time));
		
//		dataset.add(buyDepth + sellDepth);
		dataset.add(market.getBuyDepth());
		dataset.add(market.getSellDepth());
		
		// 2022/2/23 BuyDetphとSellDepthは見せ玉が反映されるが，Depthは反映されない．
		dataset.add((market.getBuyDepth() + market.getSellDepth())/2);
		dataset.add(calcBuyDepth(time, market, param));
		dataset.add(calcSellDepth(time, market, param));
		
		//		
//		// NA(その期に注文を出したNA）
//		dataset.add("ExpectedPriceOfNA");	// expectedPriceListに対応
//		dataset.add("OrderPirceOfNA");	// adjustedNAOrderPriceListに対応
//		dataset.add("OrderTypeOfNA");	// tradeFlagに対応
		
/*	22/3/9 メモリ使用削減		
		dataset.add(nagent.getExpectedPrice());
		dataset.add(nagent.getAdjustedNAOrderPrice());
		dataset.add(nagent.getTradeFlag());	// doubleではなくStringがよい
*/
		
//
//		// HFT
//		dataset.add("HFTPosition");
//		dataset.add("HFTPerformance");
		
//		// HFT ポジション影響度
//		dataset.add("PositionWeight");
//		dataset.add("OrderBookImbalanceWeight");
//		dataset.add("HFTFairValue");
//		dataset.add("BuyOrderPriceOfHFT");	// hftBuyOrderPriceListに対応
//		dataset.add("SellOrderPriceOfHFT");	// hftSellOrderPriceListに対応
//		dataset.add("SpreadOfHFT");	// hftSpreadListに対応
		// 2024/09/03 HFTは使用しないためコメントアウト
		// dataset.add(market.getHftPosition());
		// dataset.add(hft.getHFTperformance(market));
/*	22/3/9 メモリ使用削減		
		dataset.add(hft.getPositionWeight(param));
		dataset.add(hft.getOrderImbalanceWeight(market, time, param));
		dataset.add(hft.getFairValue());
		dataset.add(market.getAdjustHFTBuyOrderPrice());
		dataset.add(market.getAdjustHFTSellOrderPrice());
		dataset.add(market.getAdjustHFTSellOrderPrice() - market.getAdjustHFTBuyOrderPrice());
*/
//		// オーダーブックインバランス
//		dataset.add("PositiveOrderBookImbalance");	// orderDepthImbalance_positiveに対応
//		dataset.add("NegativeOrderBookImbalance");	// orderDepthImbalance_negativeに対応
		dataset.add(calcOrderDepthImbalancePositive(time, market, param));// doubleではなくintがよい
		dataset.add(calcOrderDepthImbalanceNegative(time, market, param));// doubleではなくintがよい
		
		dataset.add((int)market.getVolume());
		
		// 2024/07/17 成行注文と指値注文の注文数
		/*
		dataset.add((int)market.getinstantVolume());
		dataset.add((int)market.getlimitVolume());
		*/

		// 2024/07/24 成行注文と指値注文を変更した注文数
		/*
		dataset.add((int)market.getInstantToLimitVolume());
		dataset.add((int)market.getLimitToInstantVolume());
		*/
		
		// 2024/09/03 Tightness, Depthを追加
		/*
		dataset.add(getTightness());
		dataset.add(getDepth());		
		*/

		return dataset;
	}

	public Map<AggregationPhase, Map<ExecAgentType, Integer>> getExecOrdersByPeriod() {
		return execOrdersByPeriod;
	}

	public Map<AggregationPhase, Map<ExecAgentType, Integer>> getBestOrdersByPeriod() {
		return bestOrdersByPeriod;
	}

	// 各エージェント別注文を集計してmapに代入する
	public void calcOrdersCount(double[] marketPrice, int endOfBookMake, int startOfMarketOrder, int endOfMarketOrder, double fundamentalPrice, int d_pf, int endTime) {

		// 第1期：急落前
		execOrdersByPeriod.put(
				AggregationPhase.ONE,
				getAgentTypeCount(endOfBookMake, startOfMarketOrder - 1, execBuyOrderAgent, execSellOrderAgent)
		);

		bestOrdersByPeriod.put(
				AggregationPhase.ONE,
				getAgentTypeCount(endOfBookMake, startOfMarketOrder - 1, bestBidOrderAgent, bestAskOrderAgent)
		);

		// 第2期：NA成行売り注文時
		execOrdersByPeriod.put(
				AggregationPhase.TWO,
				getAgentTypeCount(startOfMarketOrder, endOfMarketOrder, execBuyOrderAgent, execSellOrderAgent)
		);
		bestOrdersByPeriod.put(
				AggregationPhase.TWO,
				getAgentTypeCount(startOfMarketOrder, endOfMarketOrder, bestBidOrderAgent, bestAskOrderAgent)
		);

		// 第3期：NA成行売り注文終了時から底値
		double bottomPrice = Arrays.stream(marketPrice).min().orElse(0.0);
		int phaseOfBottomPrice = 0;
		for(int i = 0; i < marketPrice.length; i++) {
			if(marketPrice[i] == bottomPrice) {
				phaseOfBottomPrice = i;
			}
		}

		execOrdersByPeriod.put(
				AggregationPhase.THREE,
				getAgentTypeCount(endOfMarketOrder + 1, phaseOfBottomPrice, execBuyOrderAgent, execSellOrderAgent)
		);
		bestOrdersByPeriod.put(
				AggregationPhase.THREE,
				getAgentTypeCount(endOfMarketOrder + 1, phaseOfBottomPrice, bestBidOrderAgent, bestAskOrderAgent)
		);

		// 第4期：底値から(ファンダ価格 - d_pf)達成時
		int phaseOfd_pf = 0;
		for(int i = phaseOfBottomPrice; i < marketPrice.length; i++) {
			if(marketPrice[i] >= fundamentalPrice - d_pf) {
				phaseOfd_pf = i;
				break;
			}
		}
		execOrdersByPeriod.put(
				AggregationPhase.FOUR,
				getAgentTypeCount(phaseOfBottomPrice + 1, phaseOfd_pf, execBuyOrderAgent, execSellOrderAgent)
		);
		bestOrdersByPeriod.put(
				AggregationPhase.FOUR,
				getAgentTypeCount(phaseOfBottomPrice + 1, phaseOfd_pf, bestBidOrderAgent, bestAskOrderAgent)
		);

		// 第5期：第4期終了から最後まで
		execOrdersByPeriod.put(
				AggregationPhase.FIVE,
				getAgentTypeCount(phaseOfd_pf + 1, endTime, execBuyOrderAgent, execSellOrderAgent)
		);
		bestOrdersByPeriod.put(
				AggregationPhase.FIVE,
				getAgentTypeCount(phaseOfd_pf + 1, endTime, bestBidOrderAgent, bestAskOrderAgent)
		);

	}

	// 各期間ごとのエージェント別注文を集計する
	private Map<ExecAgentType, Integer> getAgentTypeCount(int start, int end, String[] buy, String[] sell) {

		Map<ExecAgentType, Integer> map = new HashMap<>();

		// 買い注文の集計
		int nBuyCount = 0;
		int hBuyCount = 0;
		for(int i = start; i < end; i++) {
			if(buy[i] == null) {
				continue;
			}
			if(buy[i].equals("N")) {
				nBuyCount++;
			} else if(buy[i].equals("H")) {
				hBuyCount++;
			}
		}
		map.put(ExecAgentType.NA_BUY, nBuyCount);
		map.put(ExecAgentType.HFT_BUY, hBuyCount);

		// 売り注文の集計
		int nSellCount = 0;
		int hSellCount = 0;

		for(int i = start; i < end; i++) {
			if(sell[i] == null) {
				continue;
			}
			if(sell[i].equals("N")) {
				nSellCount++;
			} else if(sell[i].equals("H")) {
				hSellCount++;
			}
		}
		map.put(ExecAgentType.NA_SELL, nSellCount);
		map.put(ExecAgentType.HFT_SELL, hSellCount);

		return map;
	}

	public double getKurt() {
		return kurt;
	}

	public double getStd() {
		return std;
	}

	public double[] getVol_clus() {
		return vol_clus;
	}

	public double[] getVolume() {
		return volume;
	}

	public double[] getTightness() {
		return tightness;
	}

	public double[] getResiliency() {
		return resiliency;
	}

	public int[] getDepth() {
		return depth;
	}

	public int[] getBuyDepth() {
		return buyDepth;
	}

	public int[] getSellDepth() {
		return sellDepth;
	}
}
