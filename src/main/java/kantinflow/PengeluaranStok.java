package kantinflow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PengeluaranStok extends Transaksi {
    private final List<RincianTransaksi> rincian;
    private boolean otomatis;

    public PengeluaranStok(LocalDate tanggal) {
        super(tanggal);
        this.rincian = new ArrayList<>();
    }

    public static PengeluaranStok dariPenyimpanan(
            LocalDate tanggal,
            List<RincianTransaksi> rincian,
            boolean otomatis) {
        if (rincian == null || rincian.isEmpty()) {
            throw new IllegalArgumentException("Rincian pengeluaran wajib ada");
        }
        PengeluaranStok pengeluaran = new PengeluaranStok(tanggal);
        pengeluaran.rincian.addAll(rincian);
        pengeluaran.otomatis = otomatis;
        return pengeluaran;
    }

    /**
     * Pengeluaran manual dari satu batch yang dipilih.
     */
    public void keluarkan(Barang barang, BatchStok batch, int jumlah) {
        pastikanBelumTercatat();
        if (barang == null) {
            throw new IllegalArgumentException("Barang wajib diisi");
        }
        if (batch == null) {
            throw new IllegalArgumentException("Batch wajib dipilih");
        }
        if (!barang.memilikiBatch(batch)) {
            throw new IllegalArgumentException("Batch bukan milik barang tersebut");
        }
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah harus lebih dari 0");
        }
        if (getTanggal().isBefore(batch.getTanggalMasuk())) {
            throw new IllegalArgumentException(
                    "Tanggal pengeluaran tidak boleh sebelum tanggal masuk batch "
                    + batch.getKode());
        }
        if (batch.getSisaJumlah() < jumlah) {
            throw new StokTidakCukupException(
                    "Stok tidak cukup: diminta " + jumlah + " " + barang.getSatuan()
                            + ", tersedia " + batch.getSisaJumlah() + " "
                            + barang.getSatuan());
        }

        batch.kurangiSisa(jumlah);
        rincian.add(new RincianTransaksi(batch, jumlah));
        this.otomatis = false;
    }

    /**
     * Pengeluaran otomatis FEFO. Seluruh alokasi disusun dan total stok layak
     * diperiksa sebelum satu batch pun dikurangi.
     */
    public void keluarkanFefo(Barang barang, int jumlah, LocalDate tanggalPengeluaran) {
        pastikanBelumTercatat();
        List<RincianTransaksi> alokasi = new FefoAllocator()
                .susun(barang, jumlah, tanggalPengeluaran);

        for (RincianTransaksi rincianAlokasi : alokasi) {
            if (tanggalPengeluaran.isBefore(
                    rincianAlokasi.getBatch().getTanggalMasuk())) {
                throw new IllegalArgumentException(
                        "Tanggal pengeluaran tidak boleh sebelum tanggal masuk batch "
                        + rincianAlokasi.getBatch().getKode());
            }
        }

        for (RincianTransaksi rincianAlokasi : alokasi) {
            rincianAlokasi.getBatch().kurangiSisa(rincianAlokasi.getJumlah());
        }
        rincian.addAll(alokasi);
        this.otomatis = true;
    }

    private void pastikanBelumTercatat() {
        if (!rincian.isEmpty()) {
            throw new IllegalStateException("Pengeluaran ini sudah dicatat");
        }
    }

    @Override
    public String getJenis() {
        return otomatis ? "Pengeluaran FEFO" : "Pengeluaran";
    }

    @Override
    public BatchStok getBatch() {
        if (rincian.isEmpty()) {
            throw new IllegalStateException("Pengeluaran belum dicatat");
        }
        return rincian.get(0).getBatch();
    }

    @Override
    public int getJumlah() {
        if (rincian.isEmpty()) {
            throw new IllegalStateException("Pengeluaran belum dicatat");
        }
        int total = 0;
        for (RincianTransaksi rincianTransaksi : rincian) {
            total += rincianTransaksi.getJumlah();
        }
        return total;
    }

    @Override
    public List<RincianTransaksi> getRincian() {
        return Collections.unmodifiableList(rincian);
    }
}
