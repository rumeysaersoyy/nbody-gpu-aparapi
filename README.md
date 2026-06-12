# N-Body Probleminin SIMD Mimarisi ile Paralelleştirilmesi

Bu proje, astrofizik ve moleküler dinamik alanlarında sıklıkla karşılaşılan **N-Body (N-Cisim)** probleminin, CPU (Seri) ve GPU (Paralel) üzerindeki performans farklarını analiz etmek amacıyla geliştirilmiştir.

## 🚀 Projenin Amacı ve Kullanılan Teknolojiler
N-Body problemi doğası gereği $O(N^2)$ zaman karmaşıklığına sahiptir. Cisim sayısı arttıkça geleneksel tek çekirdekli CPU işlemciler darboğaza girmektedir. Bu projede:
* **Java** tabanlı **Aparapi** kütüphanesi kullanılarak kod çalışma zamanında (JIT) **OpenCL** diline çevrilmiştir.
* Simülasyon yükü ekran kartındaki (GPU) binlerce çekirdeğe dağıtılarak **SIMD (Single Instruction, Multiple Data)** mimarisinin gücü kullanılmıştır.
* Donanımsal yerel iş grubu (WorkGroup) limitleri `Range.create(N, 256)` fonksiyonu ile aşılarak büyük veri kümeleri işlenmiştir.

## 📊 Performans ve Hızlanma (Speedup) Analizi
Projede donanım olarak NVIDIA GPU kullanılmış ve N=20.480 adet cisim için testler gerçekleştirilmiştir. Elde edilen sonuçlar uygulamanın başarısını kanıtlamaktadır:

| Cisim Sayısı (N) | Seri (CPU) Süresi | Paralel (GPU) Süresi | Durum |
| :--- | :--- | :--- | :--- |
| **100** | 8 ms | 2173 ms | *Overhead (İletişim Maliyeti)* nedeniyle yavaşlama. |
| **10.240** | 57 ms | 1715 ms | Yavaşlama devam ediyor. |
| **20.480** | 17.452 ms | 2906 ms | **~6 Kat (6.005x) Hızlanma (Speedup)** |

**Sonuç:** Küçük veri kümelerinde verilerin RAM'den VRAM'e aktarılması ve JIT derlemesi overhead (maliyet) yaratırken; büyük veri kümelerinde GPU, devasa veri transfer maliyetini tamamen amorti ederek CPU'ya karşı ezici bir üstünlük sağlamıştır.

## 📂 Dosya Yapısı
* `App.java`: Hem seri hem de paralel hesaplamanın yapıldığı, GPU kernel atamalarının ve donanım limiti çözümlerinin yer aldığı ana kaynak kodu.
* `NBody_Rapor_RumeysaErsoy.pdf`: Projenin matematiksel altyapısını, kullanılan fonksiyonları ve performans analizlerini içeren detaylı akademik rapor.
