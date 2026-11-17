package kantinflow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Barang {
    private final int id;
    private final String nama;
    private final String satuan;
    private final List<BatchStok> daftarBatch;
    private int stokMinimum;

    public Barang(String nama, String satuan) {
        this(0, nama, satuan, 0);
    }

    public Barang(int id, String nama, String satuan) {
        this(id, nama, satuan, 0);
    }

    public Barang(int id, String nama, String satuan, int stokMinimum) {
        if (id < 0) {
            throw new IllegalArgumentException("ID barang tidak boleh negatif");
        }
        if (nama == null || nama.isBlank()) {
            throw new IllegalArgumentException("Nama barang wajib diisi");
        }
        if (satuan == null || satuan.isBlank()) {
            throw new IllegalArgumentException("Satuan wajib diisi");
        }
        if (stokMinimum < 0) {
            throw new IllegalArgumentException("Stok minimum tidak boleh negatif");
        }

        this.id = id;
        this.nama = nama;
        this.satuan = satuan;
        this.stokMinimum = stokMinimum;
        this.daftarBatch = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public int hitungSaldo() {
        int saldo = 0;
        for (BatchStok batch : daftarBatch) {
            saldo += batch.getSisaJumlah();
        }
        return saldo;
    }

    public int hitungStokLayak(LocalDate tanggal) {
        if (tanggal == null) {
            throw new IllegalArgumentException("Tanggal pengecekan wajib diisi");
        }
        int layak = 0;
        for (BatchStok batch : daftarBatch) {
            if (batch.layakPada(tanggal)) {
                layak += batch.getSisaJumlah();
            }
        }
        return layak;
    }

    public int getStokMinimum() {
        return stokMinimum;
    }

    public void setStokMinimum(int stokMinimum) {
        if (stokMinimum < 0) {
            throw new IllegalArgumentException("Stok minimum tidak boleh negatif");
        }
        this.stokMinimum = stokMinimum;
    }

    public void tambahBatch(BatchStok batch) {
        if (batch == null) {
            throw new IllegalArgumentException("Batch tidak boleh null");
        }
        daftarBatch.add(batch);
    }

    public List<BatchStok> getDaftarBatch() {
        return Collections.unmodifiableList(daftarBatch);
    }

    public boolean memilikiBatch(BatchStok batch) {
        return batch != null && daftarBatch.contains(batch);
    }

    public int jumlahBatch() {
        return daftarBatch.size();
    }

    public String getNama() {
        return nama;
    }

    public String getSatuan() {
        return satuan;
    }
}
