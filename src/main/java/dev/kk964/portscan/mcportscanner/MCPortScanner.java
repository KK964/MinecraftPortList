package dev.kk964.portscan.mcportscanner;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public final class MCPortScanner extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        PluginCommand cmd = getServer().getPluginCommand("ports");
        if (cmd == null) throw new RuntimeException("Ports command is null!");
        cmd.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Invalid command usage. Format the command like \"/ports <start> <end> [open=true]\"");
            return false;
        }

        String startS = args[0];
        String endS = args[1];
        String openS = "true";

        int portStart = 0;
        int portEnd = 0;
        boolean returnOpen = true;

        if (args.length > 2) {
            openS = args[2];
        }

        returnOpen = Boolean.parseBoolean(openS);

        try {
            portStart = Integer.parseInt(startS);
            portEnd = Integer.parseInt(endS);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid Port Start, or Port End supplied. Please use numbers between 1024 and 49151");
            return false;
        }

        if (portStart < 1024 || portStart > 49151) {
            sender.sendMessage(ChatColor.RED + "Invalid Port Start supplied. Please use numbers between 1024 and 49151");
            return false;
        }

        if (portEnd < 1024 || portEnd > 49151) {
            sender.sendMessage(ChatColor.RED + "Invalid Port End supplied. Please use numbers between 1024 and 49151");
            return false;
        }

        if (portEnd < portStart) {
            sender.sendMessage(ChatColor.RED + "Port End cannot be less than Port Start");
            return false;
        }

        ExecutorService service = Executors.newCachedThreadPool();
        int timeout = 500;
        List<Future<ScanResult>> futures = new ArrayList<>();

        for (int i = portStart; i < portEnd; i++) {
            futures.add(CheckPortOpen(service, i, timeout));
        }

        try {
            service.awaitTermination(1000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            sender.sendMessage(ChatColor.RED + "Failed to get ports, check logs for more details.");
            e.printStackTrace();
            return false;
        }

        List<ScanResult> results = new ArrayList<>();

        for (Future<ScanResult> future : futures) {
            ScanResult res;
            try {
                res = future.get();
            } catch (java.lang.InterruptedException | java.util.concurrent.ExecutionException e) {
                sender.sendMessage(ChatColor.RED + "Failed to get ports, check logs for more details.");
                e.printStackTrace();
                return false;
            }

            if (returnOpen && res.isOpen || !returnOpen && !res.isOpen) {
                results.add(res);
            }
        }

        sender.sendMessage(ChatColor.GREEN + String.format("Finished finding %d %s Ports:", results.size(), returnOpen ? "Open (in use)" : "Closed (available)"));

        for (ScanResult res : results) {
            sender.sendMessage(ChatColor.GREEN + "" + res.port);
        }

        return true;
    }

    public Future<ScanResult> CheckPortOpen(ExecutorService es, int port, int timeout) {
        return es.submit(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("127.0.0.1", port), timeout);
                socket.close();
                return new ScanResult(port, true);
            } catch (Exception ex) {
                return new ScanResult(port, false);
            }
        });
    }

    public class ScanResult {
        public int port;
        public boolean isOpen;
        public ScanResult(int port, boolean isOpen) {
            this.port = port;
            this.isOpen = isOpen;
        }
    }
}
