import java.util.*;
import java.util.stream.IntStream;

/**
 * 21/6/24
 * 全シミュレーション終了時の平均を集計するクラス
 * @author 星野
 */
public class Result {

    private final Application[] apps;
    private final String date;

    /**
     * コンストラクタ
     * @param apps 集計したいアプリケーション（スレッド）の配列
     * @param date 実行日時（フォルダ名）
     */
    public Result(Application[] apps, String date) {
        this.apps = apps;
        this.date = date;
    }

    /**
     * 最終データの集計と出力
     */
    // 21/12/30 引数param追加
    public void aggregate(int t_end) {

        // 出力用のデータ作成
        // Map に格納した順番で出力
        ExportCSV csv = new ExportCSV(date);

        // 何のHFTがあるか調べる
        // Setは同じ要素を入れるとはぶかれるので，違うHFTのみsetに入っていく
        Set<Flag.HFT> set = new LinkedHashSet<>();
        for (Application app : apps) {
          set.add(app.getHft_flag());
        }

        // HFT種類別にデータを分ける
        Map<Flag.HFT, List<Application>> resultData = new LinkedHashMap<>();

        // HFTの種類分
        for(Flag.HFT flag : set) {
          List<Application> applicationList = new ArrayList<>();
          // アプリケーションから
          for(Application app : apps) {
            // HFTの対応した種類だけ取り出す
            if(flag == app.getHft_flag()) {
              applicationList.add(app);
            }
          }
          resultData.put(flag, applicationList);
        }

        // HFTの種類ごとに平均を計算して出力
        resultData.forEach((key, value) -> {
            Map<String, Object> results = new LinkedHashMap<>();
            List<Data> data = value.stream().map(Application::getData).toList();
            List<Parameter> param = value.stream().map(Application::getParam).toList();
            List<Market> markets = value.stream().map(Application::getMarket).toList();
            results.put("std", data.stream().mapToDouble(Data::getStd).average().orElse(0.0));
            results.put("kurt", data.stream().mapToDouble(Data::getKurt).average().orElse(0.0));
            int lag = 6; // 0 ~ 5
            for(int i = 0; i < lag; i++) {
              double sumLag = 0.0;
              for (Data lagData : data) {
                sumLag += lagData.getVol_clus()[i];
              }
              results.put("lag " + i, sumLag / data.size());
            }
            results.put("volume", data.stream()
                .mapToDouble(m -> m.getVolume()[m.getVolume().length - 1])
                .average().orElse(0.0));
            results.put("tightness", data.stream()
                .mapToDouble(m -> Arrays.stream(m.getTightness())
                    .average().orElse(0.0))
                .average().orElse(0.0));
            results.put("resiliency", data.stream()
                .mapToDouble(m -> Arrays.stream(m.getResiliency())
                    .average().orElse(0.0))
                .average().orElse(0.0));
            results.put("buydepth", data.stream()
                .mapToDouble(m -> Arrays.stream(m.getBuyDepth())
                    .average().orElse(0.0))
                .average().orElse(0.0));
            results.put("selldepth", data.stream()
                .mapToDouble(m -> Arrays.stream(m.getSellDepth())
                    .average().orElse(0.0))
                .average().orElse(0.0));
            results.put("depth", data.stream()
                .mapToDouble(m -> Arrays.stream(m.getDepth())
                    .average().orElse(0.0))
                .average().orElse(0.0));
            results.put("marketPrice", markets.stream()
                .mapToDouble(m -> Arrays.stream(m.getMarketPriceArray())
                    .average().orElse(0.0))
                .average().orElse(0.0));
            results.put("Volatility", calcVolatilityAve(data, param));
            results.put("HFT_Position", calcHftPositionAve(data));
            results.put("HFT_Performance", calcHftPerformanceAve(data, csv.getExportEnd(t_end)));
            results.put("DifferenceODI", calcDifferenceODI(data));
            if(param.get(0).getCnt_mrkt_ordr_strt() < param.get(0).getT_end()) {
                countingNumberOfOrdersByPeriod(data, results);
            }
            csv.exportCsv(results, String.format("%s_%s.csv", key, date));
        });

    }

    // 21/12/28
    private double calcDifferenceODI(List<Data> data) {
        return data.stream().map(Data::getDifferenceODI)
                .mapToDouble(differenceODI -> differenceODI)
                .average()
                .orElse(0.0);
    }

    /**
     * 21/6/28変更
     * 各試行のHFTパフォーマンスの平均を計算
     * @param data HFTパフォーマンスのリストを持つData型の配列
     * @param endTime csv出力の終了期
     * @return HFTパフォーマンス（各試行の平均）
     */
    private double calcHftPerformanceAve(List<Data> data, int endTime) {
        return data.stream().map(Data::getHftPerformance)
                .mapToDouble(performances -> performances[endTime])
                .average()
                .orElse(0.0);
    }

    /**
     * 21/6/28変更
     * 各試行のHFTポジションの平均を計算
     * @param data HFTポジションのリストを持つData型の配列
     * @param endTime csv出力の終了期
     * @return HFTポジション（各試行の平均）
     */
//    private double calcHftPositionAve(Data[] data, int endTime) {
//        return Arrays.stream(data).map(Data::getHftPosition)
//                .mapToDouble(positions -> positions.get(endTime))
//                .average()
//                .orElse(0.0);
//    }
//
    /**
     * 21/12/25変更
     * 各試行のHFTポジションの平均を計算
     * @param data HFTポジションのリストを持つData型の配列
     * @return HFTポジション（各試行内の平均の（試行間の）平均）
     */
    private double calcHftPositionAve(List<Data> data) {
        return data.stream().map(Data::getHftPosition)
                .mapToDouble(positions -> Arrays.stream(positions)
                        .mapToDouble(m -> m)
                        // 1試行の平均を計算
                        .average()
                        .orElse(0.0))
                // 各試行の平均を計算
                .average()
                .orElse(0.0);
    }

    // ボラティリティの平均を計算
    private double calcVolatilityAve(List<Data> data, List<Parameter> params) {
        return IntStream.range(0, data.size())
            .mapToDouble((int i) -> data.get(i).calcStd(params.get(i), params.get(0).getT_end()))
            .average()
            .orElse(0.0);
    }


    // エージェント別注文の割合を計算して出力用に成形する
    private void countingNumberOfOrdersByPeriod(List<Data> data, Map<String, Object> map) {

        // 取引が成立したときの注文板上のエージェントごとの期間別注文数とその割合
        map.put("""
            \s
            Number of orders in the order book by agent types that matched to a new order
            ,,NA(count),NA(rate),HFT(count),HFT(rate)""", "");

        for(Data.AggregationPhase phase : Data.AggregationPhase.values()) {
            double naBuy = 0.0;
            double naSell = 0.0;
            double hftBuy = 0.0;
            double hftSell = 0.0;

            int i;
            for(i = 0; i < data.size(); i++) {
                Map<Data.ExecAgentType, Integer> phaseData = data.get(i).getExecOrdersByPeriod().get(phase);
                naBuy += phaseData.get(Data.ExecAgentType.NA_BUY).doubleValue();
                hftBuy += phaseData.get(Data.ExecAgentType.HFT_BUY).doubleValue();
                naSell += phaseData.get(Data.ExecAgentType.NA_SELL).doubleValue();
                hftSell += phaseData.get(Data.ExecAgentType.HFT_SELL).doubleValue();
            }
            naBuy /= i;
            naSell /= i;
            hftBuy /= i;
            hftSell /= i;

            double totalBuy = naBuy + hftBuy;
            double totalSell = naSell + hftSell;

            map.put("Phase " + phase + ",buy", String.format("%f,%f,%f,%f", naBuy, naBuy/totalBuy, hftBuy, hftBuy/totalBuy));
            map.put("Phase " + phase + ",sell", String.format("%f,%f,%f,%f", naSell, naSell/totalSell, hftSell, hftSell/totalSell));

        }

        // 期間別のエージェントごとの最良気配注文数とその割合
        map.put("""
            \s
            Number of best bid/ask orders in the order book by agent types
            ,,NA(count),NA(rate),HFT(count),HFT(rate)""", "");

        for(Data.AggregationPhase phase : Data.AggregationPhase.values()) {
            double naBuy = 0.0;
            double naSell = 0.0;
            double hftBuy = 0.0;
            double hftSell = 0.0;

            int i;
            for(i = 0; i < data.size(); i++) {
                Map<Data.ExecAgentType, Integer> phaseData = data.get(i).getBestOrdersByPeriod().get(phase);
                naBuy += phaseData.get(Data.ExecAgentType.NA_BUY).doubleValue();
                hftBuy += phaseData.get(Data.ExecAgentType.HFT_BUY).doubleValue();
                naSell += phaseData.get(Data.ExecAgentType.NA_SELL).doubleValue();
                hftSell += phaseData.get(Data.ExecAgentType.HFT_SELL).doubleValue();
            }
            naBuy /= i;
            naSell /= i;
            hftBuy /= i;
            hftSell /= i;

            double totalBuy = naBuy + hftBuy;
            double totalSell = naSell + hftSell;

            map.put("Phase " + phase + ",buy ", String.format("%f,%f,%f,%f", naBuy, naBuy/totalBuy, hftBuy, hftBuy/totalBuy));
            map.put("Phase " + phase + ",sell ", String.format("%f,%f,%f,%f", naSell, naSell/totalSell, hftSell, hftSell/totalSell));

        }

    }

}
