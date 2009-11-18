data = [
%Time Spent Seeding	Note	Time	Messages	Leechers	Chunks
0	true	true	2441	3331	0	10
50	true	true	2458	3128	0	10
100	true	true	2181	2770	0	10
150	true	true	2441	2649	0	10
200	true	true	2223	2515	0	10
500	true	true	2074	2235	0	10
1000	true	true	2289	2189	0	10

0	false	true	200	424	83	0
50	false	true	1085	583	99	0
100	false	true	1138	3918	99	3
150	false	true	1200	6912	99	6
200	false	true	1233	7670	99	7
250	false	true	1285	9490	99	9
300	false	true	1179	2096	0	10
350	false	true	1070	2018	0	10
400	false	true	1129	1955	0	10
450	false	true	1204	1971	0	10
500	false	true	1194	1920	0	10

0	false	false	38	595	99	0
50	false	false	1070	2781	99	0
100	false	false	1118	13200	99	5
150	false	false	1171	14978	99	7
200	false	false	1220	17332	99	9
250	false	false	7291	4717	0	10
];

% Extract
x1 = data(1:7,1);
yTime1 = data(1:7,4);
yPieces1 = data(1:7,7);
x2 = data(8:18,1);
yTime2 = data(8:18,4);
yPieces2 = data(8:18,7);
x3 = data(19:24,1);
yTime3 = data(19:24,4);
yPieces3 = data(19:24,7);

% Plot
figure1 = figure();
axes1 = axes('Parent',figure1);
line(x1,yTime1,'Parent',axes1,'Marker','s','Color',[0 0 1],'DisplayName','Reliable Seeder / Instant Start');
line(x2,yTime2,'Parent',axes1,'Marker','o','Color',[1 0 0],'DisplayName','Unreliable Seeder / Instant Start');
%line(x3,yTime3,'Parent',axes1,'Marker','d','Color',[0 1 0],'DisplayName','Unreliable Seeder / Synchronized Start');
ylim(axes1,[0 3000]);
box(axes1,'on');
grid(axes1,'on');
xlabel('Time spent seeding [s]');
ylabel('TTC (Time to completion) [s]');
legend(axes1,'show');

% Export
set(figure1,'PaperPositionMode','auto');
set(figure1,'PaperSize',[16 12]); 
set(figure1,'PaperUnits','centimeters'); 
print(figure1,'-loose','-dpdf','../figs/Experiment6_TTC');
%close(figure1);

% Plot
figure2 = figure();
axes2 = axes('Parent',figure2);
line(x1,yPieces1,'Parent',axes2,'Marker','s','Color',[0 0 1],'DisplayName','Reliable Seeder / Instant Start');
line(x2,yPieces2,'Parent',axes2,'Marker','o','Color',[1 0 0],'DisplayName','Unreliable Seeder / Instant Start');
%line(x3,yPieces3,'Parent',axes2,'Marker','d','Color',[0 1 0],'DisplayName','Unreliable Seeder / Synchronized Start');
ylim(axes2,[0 13]);
box(axes2,'on');
grid(axes2,'on');
xlabel('Time spent seeding [s]');
ylabel('Pieces downloaded [n]');
legend(axes2,'show');

% Export
set(figure2,'PaperPositionMode','auto');
set(figure2,'PaperSize',[16 12]); 
set(figure2,'PaperUnits','centimeters'); 
print(figure2,'-loose','-dpdf','../figs/Experiment6_PIECES');
%close(figure2);
