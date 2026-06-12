package com.nbody;

import com.aparapi.Kernel;
import java.util.Random;
import com.aparapi.Range;

/**
 * n-body prblemi paralel çözüm uygulaması
 * @author rumeysa
 */
public class App {
    public static void main(String[] args) {
        // hızlanmayı test etmk için kullancğım n (cisim) sayısı.
        // proje raporuna ekleyeceğim tablo için bu degeri 100, 500, 1000, 5000 yapıp sonçları not alıcam.
        final int N = 20480; 
        final int adimSayisi = 10; // simulasyonun kac adım ilerleyceği.

        // n-body problemindeki cismlerin özellkleri. 
        // aparapi obje dizilerini dsteklemediği için mecburen konum hız ve kütle biligilerini ayrı ayrı primitive dizler olaraka tanımladım.
        final float[] x = new float[N];
        final float[] y = new float[N];
        final float[] vx = new float[N];
        final float[] vy = new float[N];
        final float[] mass = new float[N];

        // cismleere baslangıç için rasgele knum ve kütle ataması yapıyorumm.
        Random rnd = new Random();
        for (int i = 0; i < N; i++) {
            x[i] = rnd.nextFloat() * 100f;
            y[i] = rnd.nextFloat() * 100f;
            vx[i] = 0f;
            vy[i] = 0f;
            mass[i] = rnd.nextFloat() * 10f + 1f; // sıfıra bölme hatası almım diye kütleyi 1 ile 11 arasında tutuyorm.
        }

        // çekm kuveti hesaplamsı için kullancğım fiziksel sabitlr.
        final float G = 6.674f; // yerçkimi sabti.
        final float dt = 0.01f; // zman adımı.
        final float eps = 0.001f; // uzaklık sıfr çıkarsa formül patlamasn diye eklidiğim çok küçük tolerans dğeri.

        System.out.println(N + " adet cisim için test başlatılıyor...\n");

        // ==========================================
        // 1. asama: seri cpu hesaplaması
        // ==========================================
        // önce priblemi normal sekilde tek çekrdekte çözüyorum ki, sornadan paralel çözmle kıyaslayıp hızlnmayı (speedup) buliyim.
        long seriBaslangic = System.currentTimeMillis();
        
        for (int step = 0; step < adimSayisi; step++) {
            for (int i = 0; i < N; i++) {
                float fx = 0f;
                float fy = 0f;
                
                // her cisim diğer tümm cisimlerle etkileşime grdiği için iç içe iki döngü vr.
                // bu yuzden islem karmaşklığı O(N^2) oluyo.
                for (int j = 0; j < N; j++) {
                    if (i != j) {
                        float dx = x[j] - x[i];
                        float dy = y[j] - y[i];
                        float distSqr = dx * dx + dy * dy + eps;
                        float dist = (float) Math.sqrt(distSqr);
                        float force = (G * mass[i] * mass[j]) / distSqr;
                        
                        fx += force * (dx / dist);
                        fy += force * (dy / dist);
                    }
                }
                // newton un haerket yasalarına göre buldugm toplaö kuvvte ile hızları ve knoumarı güncelliyorm.
                vx[i] += (fx / mass[i]) * dt;
                vy[i] += (fy / mass[i]) * dt;
                x[i] += vx[i] * dt;
                y[i] += vy[i] * dt;
            }
        }
        long seriBitis = System.currentTimeMillis();
        long seriSure = seriBitis - seriBaslangic;
        System.out.println("Seri (CPU) Çalışma Süresi: " + seriSure + " ms");

        // ==========================================
        // 2. asmaa: paralel gpu aparapi hesaplaması
        // ==========================================
        // simdi aynı problmei aparapi kutuphanesiyle simd miarisine uygun olrak ekrn kartında paralel çözcem.
        
        Kernel nBodyKernel = new Kernel() {
            @Override
            public void run() {
                // seri koddaki en ds döngüyü kladrdım. onun yerne gpudaki her bir thread e denk gelen global id yi kullnyrm. 
                // her tghread sadce tek bir i cismile ileilgilencek.S
                int i = getGlobalId();
                float fx = 0f;
                float fy = 0f;

                for (int j = 0; j < N; j++) {
                    if (i != j) {
                        float dx = x[j] - x[i];
                        float dy = y[j] - y[i];
                        float distSqr = dx * dx + dy * dy + eps;
                        
                        // aparaipnin math.sqrt fonskiyonunu arkada dogrudan opencl kdduna cevirebilmsi isimi ck kolaylastrıyor.
                        float dist = (float) Math.sqrt(distSqr); 
                        float force = (G * mass[i] * mass[j]) / distSqr;
                        
                        fx += force * (dx / dist);
                        fy += force * (dy / dist);
                    }
                }
                vx[i] += (fx / mass[i]) * dt;
                vy[i] += (fy / mass[i]) * dt;
                x[i] += vx[i] * dt;
                y[i] += vy[i] * dt;
            }
        };

        long paralelBaslangic = System.currentTimeMillis();
        
        for (int step = 0; step < adimSayisi; step++) {
            // kernelı n adet trhead ile çalıstırıyrm. böylce bütün cisimlerin kuvet hesabı aynı anda ypılıyor.
            nBodyKernel.execute(Range.create(N, 256));
        }
        
        long paralelBitis = System.currentTimeMillis();
        long paralelSure = paralelBitis - paralelBaslangic;
        System.out.println("Paralel (GPU) Çalışma Süresi: " + paralelSure + " ms");
        System.out.println("Kullanılan Cihaz: " + nBodyKernel.getTargetDevice().getShortDescription());

        // ==========================================
        // 3. asama: hızlnama speeudp katsayısı
        // ==========================================
        // proje istrlerinde hcoanın özellikle bklediği hzılanma katsaysını hesplayıp ekrana yzdıryoum.
        // hizlanma = seri sure / paralel surre formuluyle buldum.
        float speedup = (float) seriSure / paralelSure;
        System.out.println("\nElde Edilen Hızlanma Katsayısı (Speedup): " + speedup);
        
        // ıslem bitkiten sonr bellek sizintisi olmsın diye kernli temizliyrum.
        nBodyKernel.dispose();
    }
}