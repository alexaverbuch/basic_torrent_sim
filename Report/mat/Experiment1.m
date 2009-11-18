data = [
%Time Download 1 Piece	Upload Slots	Download Slots	Protocol Time	Messages
%	--- ?Download=12? ---			
20	1	12	1164	119033
40	2	12	1019	100861
60	3	12	1029	95961
80	4	12	962	87891
100	5	12	1216	101865
120	6	12	1072	87875
140	7	12	1358	112269
160	8	12	1124	88047
180	9	12	1354	105681
200	10	12	1297	95611
220	11	12	1914	145357
240	12	12	1834	129701
%	--- ?Upload=4? ---			
80	4	1	1941	11149
80	4	2	1313	15987
80	4	3	1092	22497
80	4	4	1135	32019
80	4	5	1047	35867
80	4	6	1081	48383
80	4	7	1067	54655
80	4	8	983	57247
80	4	9	1064	73539
80	4	10	944	71353
];

% Extract
x1 = data(1:12,2);
yTime1 = data(1:12,4);
%yMessages1 = data(1:4,5);
x2 = data(13:22,3);
yTime2 = data(13:22,4);
%yMessages2 = data(5:8,5);

% Plot
figure1 = figure();
axes1 = axes('Parent',figure1,'XTick',[1 2 3 4 5 6 7 8 9 10 11 12]);
line(x1,yTime1,'Parent',axes1,'Marker','s','Color',[0 0 1],'DisplayName','12 Download slots');
ylim(axes1,[0 2200]);
box(axes1,'on');
grid(axes1,'on');
xlabel('Upload slots [n]');
ylabel('TTC (Time to completion) [s]');
legend(axes1,'show');

% Export
set(figure1,'PaperPositionMode','auto');
set(figure1,'PaperSize',[16 12]); 
set(figure1,'PaperUnits','centimeters'); 
print(figure1,'-loose','-dpdf','../figs/Experiment1_TTC');
%close(figure1);

% Plot
figure2 = figure();
axes2 = axes('Parent',figure2,'XTick',[1 2 3 4 5 6 7 8 9 10 11 12]);
line(x2,yTime2,'Parent',axes2,'Marker','s','Color',[0 0 1],'DisplayName','4 Upload slots');
ylim(axes2,[0 2200]);
box(axes2,'on');
grid(axes2,'on');
xlabel('Download slots [n]');
ylabel('TTC (Time to completion) [s]');
legend(axes2,'show');

% Export
set(figure2,'PaperPositionMode','auto');
set(figure2,'PaperSize',[16 12]); 
set(figure2,'PaperUnits','centimeters'); 
print(figure2,'-loose','-dpdf','../figs/Experiment1_MSG');
%close(figure2);
