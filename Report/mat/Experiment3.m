data = [
%Upload Bandwidth	Time	Messages	Minimum Time To Download 1-4 Pieces
32	1135	32019	80
64	726	21525	40
128	601	19979	20
256	499	16535	10
512	452	15585	5
1024	450	16145	2.5
];

%% Extract
x = data(:,1);
yTime = data(:,2);
%yMessages = data(:,3);
yMinTime = data(:,4);

%% Plot
figure1 = figure();
%axes1 = axes('Parent',figure1,'XTick',[32 64 128 256 512 1024],'XScale','log','XMinorTick','on','XMinorGrid','on');
axes1 = axes('Parent',figure1,'XTick',[32 64 128 256 512 1024]);
line(x,yTime,'Parent',axes1,'Marker','s','Color',[0 0 1],'DisplayName','TTC');
ylim(axes1,[0 1200]);
box(axes1,'on');
grid(axes1,'on');
xlabel('Upload bandwidth [Kb/s]');
ylabel('TTC (Time to completion) [s]');
%legend(axes1,'show');

%% Export
set(figure1,'PaperPositionMode','auto');
set(figure1,'PaperSize',[16 12]); 
set(figure1,'PaperUnits','centimeters'); 
print(figure1,'-loose','-dpdf','../figs/Experiment3_TTC');
%close(figure1);

figure2 = figure();
%axes2 = axes('Parent',figure2,'XTick',[32 64 128 256 512 1024],'XScale','log','XMinorTick','on','XMinorGrid','on');
axes2 = axes('Parent',figure2,'XTick',[32 64 128 256 512 1024]);
line(x,yMinTime,'Parent',axes2,'Marker','s','Color',[0 0 1],'DisplayName','min. TTC per Piece');
ylim(axes2,[0 100]);
box(axes2,'on');
grid(axes2,'on');
xlabel('Upload bandwidth [Kb/s]');
ylabel('min. TTC (Time to completion) [s]');
%legend(axes2,'show');

%% Export
set(figure2,'PaperPositionMode','auto');
set(figure2,'PaperSize',[16 12]); 
set(figure2,'PaperUnits','centimeters'); 
print(figure2,'-loose','-dpdf','../figs/Experiment3_min_TTC');
%close(figure2);

%% Plot
%figure2 = figure();
%axes2 = axes('Parent',figure2);
%line(x,yMessages,'Parent',axes2,'Marker','s','Color',[0 0 1],'DisplayName','Messages');
%ylim(axes2,[0 34000]);
%box(axes2,'on');
%grid(axes2,'on');
%xlabel('Upload bandwidth [Kb/s]');
%ylabel('Messages [n]');
%%legend(axes2,'show');
%
%% Export
%set(figure2,'PaperPositionMode','auto');
%set(figure2,'PaperSize',[16 12]); 
%set(figure2,'PaperUnits','centimeters'); 
%print(figure2,'-loose','-dpdf','../figs/Experiment3_MSG');
%%close(figure2);
