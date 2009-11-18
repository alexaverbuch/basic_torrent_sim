data = [
%Churn	Reliable Seeder	Time	Messages	Seeders	Leechers	Joins	Failures
0	true	1277	10291	101	0	101	0
5	true	1900	10066	101	0	106	5
10	true	2037	11206	111	0	116	5
15	true	2250	11541	113	0	122	9
20	true	1951	11624	119	0	130	11
25	true	2507	11622	118	0	134	16
30	true	2632	10550	105	0	132	27
35	true	2696	11643	109	0	139	30
40	true	2675	12308	126	0	152	26
45	true	2923	10631	105	0	148	43
50	true	2948	10463	101	0	148	47
55	true	2757	10965	107	0	157	50
60	true	3105	11776	119	0	168	49
65	true	3279	11656	112	0	169	57
70	true	3297	10851	99	0	169	70
0	false	1269	10542	100	0	100	0
10	false	2028	11312	110	0	115	5
%20	false	1965	9756	0	119	129	10
%50	false	2024	509	0	98	147	49
50	false	2824	9971	96	0	147	51
];

% Extract
x1 = data(1:15,1);
yTime1 = data(1:15,3);
x2 = data(16:18,1);
yTime2 = data(16:18,3);

% Plot
figure1 = figure();
axes1 = axes('Parent',figure1);
line(x1,yTime1,'Parent',axes1,'Marker','s','Color',[0 0 1],'DisplayName','Reliable seeder');
%line(x2,yTime2,'Parent',axes1,'Marker','o','Color',[1 0 0],'DisplayName','Not reliable seeder');
ylim(axes1,[0 3500]);
box(axes1,'on');
grid(axes1,'on');
xlabel('Churn rate [%]');
ylabel('TTC (Time to completion) [s]');
%legend(axes1,'show');

% Export
set(figure1,'PaperPositionMode','auto');
set(figure1,'PaperSize',[16 12]); 
set(figure1,'PaperUnits','centimeters'); 
print(figure1,'-loose','-dpdf','../figs/Experiment5_TTC');
%close(figure1);

%% Plot
%figure2 = figure();
%axes2 = axes('Parent',figure2,'YTick',[0 2500 5000 7500 10000 12500 15000]);
%line(x,yMessages1,'Parent',axes2,'Marker','s','Color',[0 0 1],'DisplayName','Reliable seeder');
%line(x,yMessages2,'Parent',axes2,'Marker','o','Color',[1 0 0],'DisplayName','No reliable seeder');
%ylim(axes2,[0 15000]);
%box(axes2,'on');
%grid(axes2,'on');
%xlabel('Churn rate [%]');
%ylabel('Messages [n]');
%legend(axes2,'show');
%
%% Export
%set(figure2,'PaperPositionMode','auto');
%set(figure2,'PaperSize',[16 12]); 
%set(figure2,'PaperUnits','centimeters'); 
%print(figure2,'-loose','-dpdf','../figs/Experiment5_MSG');
%%close(figure2);
