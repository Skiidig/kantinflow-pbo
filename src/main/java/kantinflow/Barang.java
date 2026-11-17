package kantinflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Barang {
    private final String nama;
    private final String satuan;
    private final List<BatchStok> daftarBatch;

    public Barang(String nama, String satuan) {
        if (nama == null || nama.isBlank()) {
            throw new IllegalArgumentException("Nama barang wajib diisi");
        }
        if (satuan == null || satuan.isBlank()) {
            throw new IllegalArgumentException("Satuan wajib diisi");
        }

        this.nama = nama;
        this.satuan = satuan;
        this.daftarBatch = new ArrayList<>();
    }

    public int hitungSaldo() {
        int saldo = 0;
        for (BatchStok batch : daftarBatch) {
            saldo += batch.getSisaJumlah();
        }
        return saldo;
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
