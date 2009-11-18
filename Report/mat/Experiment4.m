data = [
%File Size = 2400			
%Chunk Count	Minimum Time To Download 1-4 Pieces	Chunk Size	Time	Messages
1	750	2400	2718	38191
2	375	1200	1889	44199
3	250	800	1595	39501
4	187.5	600	1266	33453
5	150	480	1365	37299
6	125	400	1202	31931
7	107.14	342.86	1056	28433
8	93.75	300	1076	29229
9	83.33	266.67	1109	30689
10	75	240	1075	30627
11	68.18	218.18	1009	27937
12	62.5	200	1000	28815
13	57.69	184.62	1056	30217
14	53.57	171.43	994	30385
15	50	160	987	29091
16	46.88	150	1045	31577
17	44.12	141.18	1009	31135
18	41.67	133.33	1041	30725
19	39.47	126.32	1032	32261
20	37.5	120	1054	32765
30	25	80	1205	39689
40	18.75	60	1319	47925
50	15	48	1565	57539
%File Size = 1200			
%Chunk Count	Minimum Time To Download 1-4 Pieces	Chunk Size	Time	Messages
1	375	1200	1436	21071
2	187.5	600	1036	25167
3	125	400	915	23235
4	93.75	300	901	23857
5	75	240	832	23301
6	62.5	200	795	22457
7	53.57	171.43	754	21069
8	46.88	150	719	20747
9	41.67	133.33	710	21709
10	37.5	120	752	23003
11	34.09	109.09	752	23011
12	31.25	100	713	22491
13	28.85	92.31	742	23641
14	26.79	85.71	733	23061
15	25	80	753	25195
16	23.44	75	770	26065
17	22.06	70.59	768	25751
18	20.83	66.67	786	26781
19	19.74	63.16	812	28317
20	18.75	60	815	28101
30	12.5	40	1018	36443
40	9.38	30	1169	45155
50	7.5	24	1334	53741
%File Size = 600			
%Chunk Count	Minimum Time To Download 1-4 Pieces	Chunk Size	Time	Messages
1	187.5	600	965	12531
2	93.75	300	675	16201
3	62.5	200	603	15607
4	46.88	150	562	15901
5	37.5	120	589	17367
6	31.25	100	527	16107
7	26.79	85.71	537	16885
8	23.44	75	569	17753
9	20.83	66.67	623	20423
10	18.75	60	575	18687
11	17.05	54.55	581	18835
12	15.63	50	576	19619
13	14.42	46.15	606	20201
14	13.39	42.86	630	21733
15	12.5	40	632	21405
16	11.72	37.5	670	23271
17	11.03	35.29	669	23791
18	10.42	33.33	679	24549
19	9.87	31.58	733	25311
20	9.38	30	750	27019
30	6.25	20	956	35633
40	4.69	15	1089	43249
50	3.75	12	1310	53283
];

% Extract
x = data(1:23,1);
yTime1 = data(1:23,4);
yTime2 = data(24:46,4);
yTime3 = data(47:69,4);
yminTime1 = data(1:23,2);
yminTime2 = data(24:46,2);
yminTime3 = data(47:69,2);

% Plot
figure1 = figure();
axes1 = axes('Parent',figure1);
line(x,yTime1,'Parent',axes1,'Marker','s','Color',[0 0 1],'DisplayName','File size of 2400');
line(x,yTime2,'Parent',axes1,'Marker','o','Color',[0 1 0],'DisplayName','File size of 1200');
line(x,yTime3,'Parent',axes1,'Marker','d','Color',[1 0 0],'DisplayName','File size of 600');
ylim(axes1,[0 3000]);
box(axes1,'on');
grid(axes1,'on');
xlabel('Piece count [n]');
ylabel('TTC (Time to completion) [s]');
legend(axes1,'show');

% Export
set(figure1,'PaperPositionMode','auto');
set(figure1,'PaperSize',[16 12]); 
set(figure1,'PaperUnits','centimeters'); 
print(figure1,'-loose','-dpdf','../figs/Experiment4_TTC');
%close(figure1);

% Plot
figure2 = figure();
axes2 = axes('Parent',figure2);
line(x,yminTime1,'Parent',axes2,'Marker','s','Color',[0 0 1],'DisplayName','File size of 2400');
line(x,yminTime2,'Parent',axes2,'Marker','o','Color',[0 1 0],'DisplayName','File size of 1200');
line(x,yminTime3,'Parent',axes2,'Marker','d','Color',[1 0 0],'DisplayName','File size of 600');
ylim(axes2,[0 200]);
box(axes2,'on');
grid(axes2,'on');
xlabel('Piece count [n]');
ylabel('min. TTC (Time to completion) [s]');
legend(axes2,'show');

% Export
set(figure2,'PaperPositionMode','auto');
set(figure2,'PaperSize',[16 12]); 
set(figure2,'PaperUnits','centimeters'); 
print(figure2,'-loose','-dpdf','../figs/Experiment4_min_TTC');
%close(figure2);
