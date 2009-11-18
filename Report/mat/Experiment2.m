data = [
%Network Size	Protocol Time	Messages
1	784	278
2	515	377
3	480	500
4	430	579
5	446	804
6	483	955
7	438	1050
8	465	1195
9	508	1466
10	481	1475
50	560	9021
100	601	19979
200	597	38915
300	614	61233
400	632	82469
500	651	106269
1000	700	232027
];

% Extract
x = data(:,1);
yTime = data(:,2);
yMessages = data(:,3);

% Plot
figure1 = figure();
axes1 = axes('Parent',figure1,'XTick',[1 2 4 6 8 10 50 100 200 500 1000],'XScale','log','XMinorTick','on','XMinorGrid','on','YMinorTick','on','YMinorGrid','on');
line(x,yTime,'Parent',axes1,'Marker','s','Color',[0 0 1],'DisplayName','TTC');
ylim(axes1,[0 800]);
box(axes1,'on');
grid(axes1,'on');
xlabel('Network size [n]');
ylabel('TTC (Time to completion) [s]');
%legend(axes1,'show');

% Export
set(figure1,'PaperPositionMode','auto');
set(figure1,'PaperSize',[16 12]); 
set(figure1,'PaperUnits','centimeters'); 
print(figure1,'-loose','-dpdf','../figs/Experiment2_TTC');
%close(figure1);

% Plot
figure2 = figure();
axes2 = axes('Parent',figure2,'XTick',[1 2 4 6 8 10 50 100 200 500 1000],'XScale','log','XMinorTick','on','XMinorGrid','on','YMinorTick','on','YMinorGrid','on');
line(x,yMessages,'Parent',axes2,'Marker','s','Color',[0 0 1],'DisplayName','Messages');
ylim(axes2,[0 240000]);
box(axes2,'on');
grid(axes2,'on');
xlabel('Network size [n]');
ylabel('Messages [n]');
%legend(axes2,'show');

% Export
set(figure2,'PaperPositionMode','auto');
set(figure2,'PaperSize',[16 12]); 
set(figure2,'PaperUnits','centimeters'); 
print(figure2,'-loose','-dpdf','../figs/Experiment2_MSG');
%close(figure2);
